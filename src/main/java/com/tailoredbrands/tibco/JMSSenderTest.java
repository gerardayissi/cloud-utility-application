package com.tailoredbrands.tibco;

import java.util.Date;

public class JMSSenderTest {

    public static void main(String[] args) {

        JMSHandler jmsHandler = new JMSHandler();
        int messageCount = 0;
        while (true) {
            jmsHandler.sendMessage("javaTestQueue", "test");
            if (messageCount++ % 100 == 0) {
                System.out.println(messageCount + " messages sent " + new Date().toString());
            }
        }
    }

}