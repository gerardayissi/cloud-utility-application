package com.tailoredbrands.tibco;

public class JMSReceiverTest {

    public static void main(String[] args) {

        JMSHandler jmsHandler = new JMSHandler();

        jmsHandler.receiveMessage("javaTestQueue");

        jmsHandler.listenForMessages("javaTestQueue");
    }

}