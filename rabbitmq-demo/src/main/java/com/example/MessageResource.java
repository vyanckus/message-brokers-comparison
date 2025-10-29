package com.example;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class MessageResource {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @PostMapping("/send/{exchange}/{key}")
    public String sendMessage(@PathVariable("exchange") String exchange,
                              @PathVariable("key") String key,
                              @RequestBody UserMessage userMessage) {
        userMessage.setTimestamp(LocalDateTime.now());
        rabbitTemplate.convertAndSend(exchange, key, userMessage);
        return "Message send to " + exchange + " with routing key " + key;
    }
}
