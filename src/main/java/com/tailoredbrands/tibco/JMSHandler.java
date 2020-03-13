package com.tailoredbrands.tibco;


import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;


public class JMSHandler {

    private static String serverUrl = "tcp://35.204.30.82:7222";
    private static String userName = "admin";
    private static String password = "";

    private final InitialContext jndiContext;
    private final Session session;
    private final MessageProducer messageProducer;
    private final Map<String, Queue> queues = new HashMap<>();

    public JMSHandler() {
        try {
            Properties props = new Properties();
            props.put(INITIAL_CONTEXT_FACTORY, "com.tibco.tibjms.naming.TibjmsInitialContextFactory");
            props.put(PROVIDER_URL, "tibjmsnaming://35.204.30.82:7222");

            jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            Connection connection = connectionFactory.createConnection(userName, password);
            connection.start();
            session = connection.createSession(false, AUTO_ACKNOWLEDGE);
            messageProducer = session.createProducer(null);
        } catch (NamingException | JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String queueName, String messageStr) {
        try {
            System.out.println("Publishing to queue '" + queueName + "'");
            Queue queue = getOrLookUpQueue(queueName);
            TextMessage msg = session.createTextMessage();
            msg.setText(messageStr);
            messageProducer.send(queue, msg);
            System.out.println("Published message: " + messageStr);
        } catch (JMSException | NamingException e) {
            throw new RuntimeException(e);
        }

    }

    public void receiveMessage(String queueName) {
        try {
            System.out.println("Receiving from queue '" + queueName + "'");
            Queue queue = getOrLookUpQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            TextMessage msg = (TextMessage) consumer.receive();
            System.out.println("Received message: " + msg.getText());
        } catch (JMSException | NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForMessages(String queueName) {
        try {
            System.out.println("Listening from queue '" + queueName + "'");
            Queue queue = getOrLookUpQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            AtomicInteger messageCount = new AtomicInteger();
            consumer.setMessageListener(message -> {
                TextMessage textMessage = (TextMessage) message;
                try {
                    System.out.println("Received event: " + textMessage.getText());
                    if (messageCount.incrementAndGet() % 1000 == 0) {
                        System.out.println(messageCount + " messages received");
                    }
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (JMSException | NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private Queue getOrLookUpQueue(String queueName) throws NamingException {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            queue = (Queue) jndiContext.lookup(queueName);
            queues.put(queueName, queue);
        }
        return queue;
    }
}
