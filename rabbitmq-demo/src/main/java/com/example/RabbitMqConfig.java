package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    Queue firstQueue() {
        return new Queue("first.queue", false);
    }

    @Bean
    Queue secondQueue() {
        return new Queue("second.queue", false);
    }

    @Bean
    Queue thirdQueue() {
        return new Queue("third.queue", false);
    }

    @Bean
    DirectExchange directExchange() {
        return new DirectExchange("our.direct.exchange");
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("our.topic.exchange");
    }

    @Bean
    Binding firstQueueToOurDirectExchange() {
        return BindingBuilder
                .bind(firstQueue())
                .to(directExchange())
                .with("first.queue");
    }

    @Bean
    Binding secondQueueToOurDirectExchange() {
        return BindingBuilder
                .bind(secondQueue())
                .to(directExchange())
                .with("second.queue");
    }

    @Bean
    Binding thirdQueueToOurTopicExchange() {
        return BindingBuilder
                .bind(thirdQueue())
                .to(topicExchange())
                .with("third.*.queue");
    }
}
