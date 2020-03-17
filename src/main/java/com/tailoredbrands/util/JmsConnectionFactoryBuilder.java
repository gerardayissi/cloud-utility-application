package com.tailoredbrands.util;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.tailoredbrands.pipeline.options.JmsConnectionProvider;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tibco.tibjms.TibjmsConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class JmsConnectionFactoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JmsConnectionFactoryBuilder.class);

    public static ConnectionFactory build(JmsToPubSubOptions options, String appName) throws JMSException {
        switch (JmsConnectionProvider.valueOf(options.getJmsProvider().toUpperCase())) {
            case TIBCO:
                LOG.info("TIBCO is used: {}", options);
                TibjmsConnectionFactory tibjmsConnectionFactory = new TibjmsConnectionFactory();
                tibjmsConnectionFactory.setServerUrl(options.getJmsServerUrl());
                tibjmsConnectionFactory.setUserName(options.getJmsUser());
                tibjmsConnectionFactory.setUserPassword(options.getJmsPassword());
                return tibjmsConnectionFactory;
            case ACTIVEMQ:
                LOG.info("ACTIVEMQ is used: {}", options);
                ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
                activeMQConnectionFactory.setBrokerURL(options.getJmsServerUrl());
                activeMQConnectionFactory.setUserName(options.getJmsUser());
                activeMQConnectionFactory.setPassword(options.getJmsPassword());
                return activeMQConnectionFactory;
            case WEBSPHERE:
                return buildWebsphereMQConnectionFactory(options, appName);
            //            case WEBSPHERE:
            //                return buildWebsphereJmsConnectionFactory(options, appName);
            default:
                throw new IllegalStateException("Unexpected JSM queue type: " + options.getJmsToPubsubPipelineType());
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

    private static ConnectionFactory buildWebsphereJmsConnectionFactory(JmsToPubSubOptions options, String appName) throws JMSException {
        LOG.info("WEBSPHERE is used: {}", options);
        final JmsConnectionFactory factory = JmsFactoryFactory
                .getInstance(WMQConstants.WMQ_PROVIDER)
                .createConnectionFactory();
        factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, options.getJmsServerUrl());
        factory.setIntProperty(WMQConstants.WMQ_PORT, options.getPort());
        factory.setStringProperty(WMQConstants.WMQ_CHANNEL, options.getChannel());
        factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, options.getQueueManager());
        factory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, appName);
        factory.setStringProperty(WMQConstants.USERID, options.getJmsUser());
        factory.setStringProperty(WMQConstants.PASSWORD, options.getJmsPassword());

        factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        //        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        return factory;

    }
}
