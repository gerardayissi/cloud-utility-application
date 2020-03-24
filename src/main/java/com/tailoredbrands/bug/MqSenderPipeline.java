package com.tailoredbrands.bug;

import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.pipeline.pattern.jms_to_pub_sub.JmsToPubSubCounter;
import com.tailoredbrands.util.JmsConnectionFactoryBuilder;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.jms.JmsIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import static com.tailoredbrands.util.Peek.increment;

public class MqSenderPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(MqSenderPipeline.class);

    public interface SendToTibcoOptions extends JmsToPubSubOptions {
        @Required
        @Default.String("gs://tst1-integration-3ca6/temp/Messages.txt")
        String getInput();

        void setInput(String value);
    }

    public static void main(String[] args) throws JMSException {
        final SendToTibcoOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(SendToTibcoOptions.class);
        final JmsToPubSubCounter counter = new JmsToPubSubCounter("SendTo" + options.getBusinessInterface());

        final ConnectionFactory connectionFactory = JmsConnectionFactoryBuilder
                .build(options, options.getBusinessInterface());

        final Pipeline pipeline = Pipeline.create(options);
        pipeline
                .apply("ReadFromGDELTFile", TextIO.read().from(options.getInput()))
                .apply("JMS records counter", increment(counter.pubsubMessagesWritten))
                .apply(JmsIO.write()
                                .withConnectionFactory(connectionFactory)
                                .withQueue(options.getJmsQueue())
                        //                .withQueue("POS.TEST.Q")
                );
        pipeline.run();
    }
}
