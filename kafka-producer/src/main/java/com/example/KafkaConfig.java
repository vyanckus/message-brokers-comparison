package com.example;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic newTopic() {
        return new NewTopic("first.kafka.topic", 1, (short) 1);
    }
}
