package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.util.JmsConnectionFactoryBuilder;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.validator.PubSubTopicValidator;
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
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.HashMap;

import static com.tailoredbrands.pipeline.error.ErrorType.JMS_PAYLOAD_EXTRACTION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.XML_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
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

        JmsToPubSubCounter counter = new JmsToPubSubCounter(options.getJmsToPubsubPipelineType());

        TupleTag<PubsubMessage> successTag = new TupleTag<PubsubMessage>() {};

        TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag = new TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>>() {};

        PCollectionList pcl = pipeline
                .apply("Read Messages from JMS",
                        JmsIO.read()
                                .withConnectionFactory(
                                        JmsConnectionFactoryBuilder.build(options, options.getJmsToPubsubPipelineType())
                                ).withQueue(options.getJmsQueue()))
                .apply("JMS records counter", increment(counter.jmsRecordsRead))
                .apply("Extract JMS message payload", extractJmsPayload())
                .apply("Process Message", JmsToPubSubMultiTopicsProcessorFactory.forType(options.getJmsToPubsubPipelineType()));

        // TODO refactored later and needed proper testing
        List<PCollection> pCollections = List.ofAll(pcl.getAll());
        String[] topics = options.getOutputTopics().get().split(",");

        PubSubTopicValidator topicValidator = new PubSubTopicValidator();

        List.of(topics).forEach(topic ->
                topicValidator.validateTopic(topic).toEither()
                        .fold(exc -> exc,
                                validTopic ->
                                        processAllPC(pCollections, successTag, failureTag, counter, validTopic)
                        )
        );

        return pipeline.run();
    }

    private static String processAllPC(List<PCollection> pcl,
                                       TupleTag<PubsubMessage> successTag,
                                       TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag,
                                       JmsToPubSubCounter counter,
                                       String topicName) {
        pcl.filter(pc ->
                // match by OrgId
                // TODO replace to OrgID
                topicName.endsWith(pc.getName())
        )
                .forEach(matchedPC ->
                        processPCollection(matchedPC, successTag, failureTag, counter, topicName)
                );
        return topicName;
    }

    private static void processPCollection(PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc,
                                           TupleTag<PubsubMessage> successTag,
                                           TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag,
                                           JmsToPubSubCounter counter,
                                           String outputTopic) {

        PCollectionTuple processed = pc
                .apply("To PubSub Message for " + pc.getName(), toPubSubMessage())
                .apply("Success | Failure", split(successTag, failureTag));

        // TODO store failures somewhere to replay later?
        processed
                .get(failureTag)
                .apply("Failures counter for " + pc.getName(), increment(counter.totalErrors))
                .apply("Log failures", logAndCountFailures(counter));

        processed
                .get(successTag)
                .setCoder(PubsubMessageWithAttributesCoder.of())
                .apply("PubSub messages counter for " + pc.getName(), increment(counter.pubsubMessagesWritten))
                .apply("Write to PubSub for " + pc.getName(), PubsubIO.writeMessages().to(outputTopic));

    }

    static MapElements<JmsRecord, Tuple2<JmsRecord, Try<String>>> extractJmsPayload() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<String>>>() {})
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
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<PubsubMessage>>>() {})
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

    private static Peek<Tuple2<JmsRecord, Try<PubsubMessage>>> logAndCountFailures(JmsToPubSubCounter counter) {
        return Peek.each(failure -> {
            ProcessingException err = (ProcessingException) failure._2.failed().get();
            val detailedCounter = Match(err.getType()).of(
                    Case($(XML_TO_OBJECT_CONVERSION_ERROR), counter.jmsRecordToObjectErrors),
                    Case($(OBJECT_TO_JSON_CONVERSION_ERROR), counter.objectToJsonErrors),
                    Case($(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR), counter.jsonToPubSubErrors),
                    Case($(), counter.untypedErrors));
            detailedCounter.inc();
            counter.totalErrors.inc();
            LOG.error(format("Failed to process Message: %s", failure._1.getPayload()), err);
        });
    }
}
