package com.vyanckus.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Конфигурация WebSocket с STOMP протоколом для Spring приложения.
 * Настраивает брокер сообщений и регистрирует WebSocket endpoints.
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Настраивает STOMP брокер сообщений.
     *
     * @param registry реестр для конфигурации брокера
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /// Включаем in-memory брокер для рассылки сообщений по topics
        registry.enableSimpleBroker("/topic");
        // Префикс для сообщений, обрабатываемых @MessageMapping методами
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Регистрирует STOMP endpoint для WebSocket соединений.
     *
     * @param registry реестр для регистрации endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint с поддержкой SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
