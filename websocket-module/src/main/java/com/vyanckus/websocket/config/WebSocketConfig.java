package com.vyanckus.websocket.config;

import com.vyanckus.config.BrokerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Конфигурация WebSocket для Spring STOMP.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final BrokerProperties.WebSocketProperties config;

    public WebSocketConfig(BrokerProperties brokerProperties) {
        this.config = brokerProperties.websocket();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Включаем простой брокер сообщений для тем (topics)
        registry.enableSimpleBroker("/topic");
        // Префикс для назначений сообщений
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем WebSocket endpoint
        registry.addEndpoint(config.path())
                .setAllowedOrigins(config.allowedOrigins() ? "*" : "http://localhost:" + config.port())
                .withSockJS(); // Поддержка SockJS для fallback
    }
}
