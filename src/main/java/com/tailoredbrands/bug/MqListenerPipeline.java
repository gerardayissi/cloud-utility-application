package com.tailoredbrands.bug;

import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.pipeline.pattern.jms_to_pub_sub.JmsToPubSubCounter;
import com.tailoredbrands.util.JmsConnectionFactoryBuilder;
import com.tailoredbrands.util.Peek;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.jms.JmsIO;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import static com.tailoredbrands.pipeline.error.ErrorType.JMS_PAYLOAD_EXTRACTION_ERROR;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

public class MqListenerPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(MqListenerPipeline.class);

    public static void main(String[] args) throws JMSException {
        final JmsToPubSubOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(JmsToPubSubOptions.class);

        final JmsToPubSubCounter counter = new JmsToPubSubCounter("SendTo" + options.getBusinessInterface());

        final ConnectionFactory connectionFactory = JmsConnectionFactoryBuilder.build(
                options, options.getBusinessInterface());

        final TupleTag<String> successTag = new TupleTag<String>() {};
        final TupleTag<Tuple2<JmsRecord, Try<String>>> failureTag = new TupleTag<Tuple2<JmsRecord, Try<String>>>() {};

        final Pipeline pipeline = Pipeline.create(options);
        pipeline
                .apply(JmsIO.read()
                        .withConnectionFactory(connectionFactory)
                        .withQueue(options.getJmsQueue()))
                //            .withQueue("POS.TEST.Q"))
                //                .withQueue("GOOGLE.CLOUD.PUBLISHER"))

                .apply("JMS records counter", increment(counter.jmsRecordsRead))
                .apply("Extract JMS message payload", MapElements
                        .into(new TypeDescriptor<Tuple2<JmsRecord, Try<String>>>() {})
                        .via(jmsRecord -> new Tuple2<>(
                                jmsRecord,
                                Try.of(jmsRecord::getPayload)
                                        .peek(xml -> LOG.info("Gotten: " + xml))
                                        .mapFailure(Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(JMS_PAYLOAD_EXTRACTION_ERROR, exc)))
                        )))
        //            .apply("Success | Failure", split(successTag, failureTag))
        ;


        //        processed
        //            .get(failureTag)
        //            .apply("Log failures", logAndCountFailures(counter));
        //
        //        processed
        //            .get(successTag)
        //            .apply(logSuccess())
        //            .apply("Success messages counter", increment(counter.pubsubMessagesWritten));

        pipeline.run();
    }

    private static Peek<Tuple2<JmsRecord, Try<String>>> logAndCountFailures(JmsToPubSubCounter counter) {
        return Peek.each(failure -> {
            ProcessingException err = (ProcessingException) failure._2.failed().get();
            val detailedCounter = Match(err.getType()).of(Case($(), counter.untypedErrors));
            detailedCounter.inc();
            counter.totalErrors.inc();
            LOG.error(format("Failed to process Message: %s", failure._1.getPayload()), err);
        });
    }

    private static Peek<String> logSuccess() {
        return Peek.each(msg -> LOG.error("Got Message: {}", msg));
    }

    static ParDo.MultiOutput<Tuple2<JmsRecord, Try<String>>, String> split(TupleTag<String> success, TupleTag<Tuple2<JmsRecord, Try<String>>> failure) {
        return ParDo.of(new DoFn<Tuple2<JmsRecord, Try<String>>, String>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                Tuple2<JmsRecord, Try<String>> element = context.element();
                if (element._2.isSuccess()) {
                    context.output(element._2.get());
                } else {
                    context.output(failure, element);
                }
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }
}
