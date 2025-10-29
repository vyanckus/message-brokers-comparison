package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageResource {

    @Autowired
    private KafkaTemplate<Long, UserMessage> kafkaTemplate;

    @PostMapping("/send/{topic}")
    public String sendMessage(@PathVariable String topic, @RequestBody UserMessage userMessage) {
        kafkaTemplate.send(topic, userMessage);
        return "Message sent to topic " + topic;
    }
}
