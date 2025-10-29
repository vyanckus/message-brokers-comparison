package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(AppKafkaListener.class);

    @KafkaListener(topics = "first.kafka.topic")
    public void onMessage(UserMessage userMessage) {
        logger.info("New message {}", userMessage);
    }
}
