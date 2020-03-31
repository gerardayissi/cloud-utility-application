package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ErrorMessage;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.util.JmsConnectionFactoryBuilder;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.validator.PubSubTopicValidator;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.io.jms.JmsIO;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.HashMap;

import static com.tailoredbrands.pipeline.error.ErrorType.*;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.*;
import static java.lang.String.format;

public class JmsToPubSubMultiTopicsPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(JmsToPubSubStreamingPipeline.class);

    public static void main(String[] args) throws JMSException {
        JmsToPubSubOptions options = PipelineOptionsFactory
            .fromArgs(args)
            .withValidation()
            .as(JmsToPubSubOptions.class);
        run(options);
    }

    public static PipelineResult run(JmsToPubSubOptions options) throws JMSException {
        Pipeline pipeline = Pipeline.create(options);

        JmsToPubSubCounter counter = new JmsToPubSubCounter(options.getBusinessInterface());

        TupleTag<PubsubMessage> successTag = new TupleTag<PubsubMessage>() {
        };

        TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag = new TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>>() {
        };

        PCollectionList pcl = pipeline
            .apply("Read Messages from JMS",
                JmsIO.read()
                    .withConnectionFactory(
                        JmsConnectionFactoryBuilder.build(options, options.getBusinessInterface())
                    ).withQueue(options.getJmsQueue()))
            .apply("JMS records counter", increment(counter.jmsRecordsRead))
            .apply("Extract JMS message payload", extractJmsPayload())
            .apply("Process Message", JmsToPubSubMultiTopicsProcessorFactory.forType(options.getBusinessInterface()));

        // TODO refactored later and needed proper testing
        List<PCollection> pCollections = List.ofAll(pcl.getAll());
        String[] topics = options.getOutputTopics().get().split(",");

        PubSubTopicValidator topicValidator = new PubSubTopicValidator();

        List.of(topics).forEach(topic ->
            topicValidator.validateTopic(topic).toEither()
                .fold(exc -> exc,
                    validTopic ->
                        processAllPC(pCollections, successTag, failureTag, counter, options, validTopic)
                )
        );

        return pipeline.run();
    }

    private static String processAllPC(List<PCollection> pcl,
                                       TupleTag<PubsubMessage> successTag,
                                       TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag,
                                       JmsToPubSubCounter counter,
                                       JmsToPubSubOptions options,
                                       String topicName) {
        pcl.filter(pc ->
            topicName.endsWith(pc.getName()))
            .forEach(matchedPC ->
                processPCollection(matchedPC, successTag, failureTag, counter, options, topicName)
            );
        return topicName;
    }

    private static void processPCollection(PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc,
                                           TupleTag<PubsubMessage> successTag,
                                           TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag,
                                           JmsToPubSubCounter counter,
                                           JmsToPubSubOptions options,
                                           String outputTopic) {

        PCollectionTuple processed = pc
            .apply("To PubSub Message for " + pc.getName(), toPubSubMessage())
            .apply("Success | Failure", split(successTag, failureTag));

        processed
            .get(failureTag)
            .apply("Log failures", logFailures(counter))
            .apply("Wrap failures", wrapFailures(options.getPatternFullName(), options.getJobName(), options.getBusinessInterface()))
            .apply("Write failures to dead letter Pubsub", PubsubIO.writeMessages().to(options.getDeadletterPubsubTopic()));

        processed
            .get(successTag)
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Count and log outbound messages", countAndLogOutbound(counter.pubsubMessagesWritten))
            .apply("Write to PubSub for " + pc.getName(), PubsubIO.writeMessages().to(outputTopic));

    }

    static MapElements<JmsRecord, Tuple2<JmsRecord, Try<String>>> extractJmsPayload() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<JmsRecord, Try<String>>>() {
            })
            .via(jmsRecord -> new Tuple2<>(
                    jmsRecord,
                    Try
                        .of(jmsRecord::getPayload)
                        .mapFailure(
                            Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(JMS_PAYLOAD_EXTRACTION_ERROR, exc))
                        )
                )
            );
    }

    static MapElements<Tuple2<JmsRecord, Try<JsonNode>>, Tuple2<JmsRecord, Try<PubsubMessage>>> toPubSubMessage() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<JmsRecord, Try<PubsubMessage>>>() {
            })
            .via(tuple2 ->
                tuple2.map2(tryJsonNode ->
                    tryJsonNode
                        .map(jsonNode -> new PubsubMessage(JsonUtils.serializeToBytes(jsonNode), new HashMap<>()))
                        .mapFailure(
                            Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR, exc))
                        )
                )
            );
    }

    static ParDo.MultiOutput<Tuple2<JmsRecord, Try<PubsubMessage>>, PubsubMessage> split(
        TupleTag<PubsubMessage> success,
        TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failure) {
        return ParDo.of(new DoFn<Tuple2<JmsRecord, Try<PubsubMessage>>, PubsubMessage>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                Tuple2<JmsRecord, Try<PubsubMessage>> element = context.element();
                if (element._2.isSuccess()) {
                    context.output(element._2.get());
                } else {
                    context.output(failure, element);
                }
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }

    private static Peek<Tuple2<JmsRecord, Try<PubsubMessage>>> logFailures(JmsToPubSubCounter counter) {
        return Peek.each(failure -> {
            val err = (ProcessingException) failure._2.failed().get();
            val detailedCounter = Match(err.getType()).of(
                Case($(JSON_TO_OBJECT_CONVERSION_ERROR), counter.jmsRecordToObjectErrors),
                Case($(XML_TO_OBJECT_CONVERSION_ERROR), counter.jmsRecordToObjectErrors),
                Case($(OBJECT_TO_JSON_CONVERSION_ERROR), counter.objectToJsonErrors),
                Case($(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR), counter.jsonToPubSubErrors),
                Case($(), counter.untypedErrors));
            detailedCounter.inc();
            counter.totalErrors.inc();
            LOG.error(format("Failed to process Message: %s", failure._1.getPayload()), err);
        });
    }

    static MapElements<Tuple2<JmsRecord, Try<PubsubMessage>>, PubsubMessage> wrapFailures(String patternFullName, String jobName, String businessInterface) {
        return MapElements
            .into(new TypeDescriptor<PubsubMessage>() {
            })
            .via(tuple2 -> {
                    val err = (ProcessingException) tuple2._2.failed().get();
                    val errorMessage = new ErrorMessage();
                    errorMessage.setRawMessage(tuple2._1.getPayload());
                    errorMessage.setErrMsg(err.getCause().getMessage());
                    errorMessage.setErrorCode(err.getType().toString());
                    errorMessage.setFullPatternId(patternFullName);
                    errorMessage.setJobId(jobName);
                    errorMessage.setShortPatternId(businessInterface);
                    errorMessage.setStacktrace(API.List(err.getCause().getStackTrace()).take(10).map(StackTraceElement::toString).mkString("\n"));
                    return new PubsubMessage(JsonUtils.serializeToBytes(JsonUtils.toJsonNode(errorMessage)), new HashMap<>());
                }
            );
    }

    private static Peek<PubsubMessage> countAndLogOutbound(Counter counter) {
        return Peek.each(pubsubMessage -> {
            counter.inc();
            LOG.info(new String(pubsubMessage.getPayload()));
        });
    }
}
