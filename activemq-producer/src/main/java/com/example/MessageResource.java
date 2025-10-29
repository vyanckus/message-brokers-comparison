package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class MessageResource {

    @Qualifier("jmsTemplateTopic")
    @Autowired
    private JmsTemplate jmsTemplateTopic;

    @Qualifier("jmsTemplate")
    @Autowired
    private JmsTemplate jmsTemplate;

    @PostMapping("/send/queue")
    public String sendToQueue(@RequestBody UserMessage userMessage) {
        userMessage.setTimestamp(LocalDateTime.now());
        jmsTemplate.convertAndSend("queue", userMessage);
        return "Message send to queue";
    }

    @PostMapping("/send/topic")
    public String sendToTopic(@RequestBody UserMessage userMessage) {
        userMessage.setTimestamp(LocalDateTime.now());
        jmsTemplateTopic.convertAndSend("topic", userMessage);
        return "Message send to topic";
    }
}
