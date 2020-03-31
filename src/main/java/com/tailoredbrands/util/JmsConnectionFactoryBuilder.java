package com.tailoredbrands.util;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.tailoredbrands.pipeline.options.JmsConnectionProvider;
import com.tailoredbrands.pipeline.options.JmsCredentials;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import com.tibco.tibjms.TibjmsConnectionFactory;
import lombok.val;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import static com.tailoredbrands.util.SecretUtils.resolveSecret;


public class JmsConnectionFactoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JmsConnectionFactoryBuilder.class);

    public static ConnectionFactory build(JmsToPubSubOptions options, String appName) throws JMSException {
        switch (JmsConnectionProvider.valueOf(options.getJmsProvider().toUpperCase())) {
            case TIBCO:
                LOG.info("TIBCO is used: {}", options);
                val tibjmsConnectionFactory = new TibjmsConnectionFactory();
                val tibJmsCredentials = resolveJmsCredentials(options.as(GcpOptions.class).getProject(), options.getJmsCredentialsSecret());
                tibjmsConnectionFactory.setServerUrl(options.getJmsServerUrl());
                tibjmsConnectionFactory.setUserName(tibJmsCredentials.getJmsUsername());
                tibjmsConnectionFactory.setUserPassword(tibJmsCredentials.getJmsPassword());
                return tibjmsConnectionFactory;
            case ACTIVEMQ:
                LOG.info("ACTIVEMQ is used: {}", options);
                val activeMQConnectionFactory = new ActiveMQConnectionFactory();
                val activemqJmsCredentials = resolveJmsCredentials(options.as(GcpOptions.class).getProject(), options.getJmsCredentialsSecret());
                activeMQConnectionFactory.setBrokerURL(options.getJmsServerUrl());
                activeMQConnectionFactory.setUserName(activemqJmsCredentials.getJmsUsername());
                activeMQConnectionFactory.setPassword(activemqJmsCredentials.getJmsPassword());
                return activeMQConnectionFactory;
            case WEBSPHERE:
                return buildWebsphereMQConnectionFactory(options, appName);
            default:
                throw new IllegalStateException("Unexpected JSM queue type: " + options.getBusinessInterface());
        }
    }

    private static ConnectionFactory buildWebsphereMQConnectionFactory(JmsToPubSubOptions options, String appName) throws JMSException {
        LOG.info("WEBSPHERE is used: {}", options);
        final MQConnectionFactory factory = new MQConnectionFactory();
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setHostName(options.getJmsServerUrl());
        factory.setPort(options.getPort());
        factory.setChannel(options.getChannel());
        factory.setQueueManager(options.getQueueManager());
        factory.setAppName(appName);
        return factory;
    }

    private static JmsCredentials resolveJmsCredentials(String project, String secret){
        return JsonUtils.deserialize(resolveSecret(project, secret), JmsCredentials.class);
    }
}
