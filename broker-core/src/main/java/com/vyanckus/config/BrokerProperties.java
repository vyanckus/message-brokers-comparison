package com.vyanckus.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

/**
 * Конфигурационные свойства для всех брокеров сообщений.
 * Все настройки опциональны - брокеры будут работать с дефолтными значениями.
 */
@ConfigurationProperties(prefix = "message.broker")
@Validated
public record BrokerProperties(

        /**
         * Настройки для ActiveMQ (опционально)
         */
        @Valid
        ActiveMQProperties activemq,

        /**
         * Настройки для RabbitMQ (опционально)
         */
        @Valid
        RabbitMQProperties rabbitmq,

        /**
         * Настройки для Kafka (опционально)
         */
        @Valid
        KafkaProperties kafka,

        /**
         * Настройки для WebSocket (опционально)
         */
        @Valid
        WebSocketProperties websocket,

        /**
         * Общие настройки для всех брокеров
         */
        @Valid
        CommonProperties common

) {

    /**
     * Настройки ActiveMQ
     */
    public record ActiveMQProperties(
            @NotBlank(message = "ActiveMQ URL cannot be blank")
            String url,

            String queue,

            String username,

            String password,

            @Min(value = 1, message = "Timeout must be at least 1ms")
            int connectionTimeoutMs
    ) {
        public ActiveMQProperties {
            if (queue == null || queue.isBlank()) {
                queue = "test.queue";
            }
            if (username == null || username.isBlank()) {
                username = "admin";
            }
            if (password == null || password.isBlank()) {
                password = "admin";
            }
            if (connectionTimeoutMs == 0) {
                connectionTimeoutMs = 5000;
            }
        }
    }

    /**
     * Настройки RabbitMQ
     */
    public record RabbitMQProperties(
            @NotBlank(message = "RabbitMQ host cannot be blank")
            String host,

            @Min(value = 1, message = "Port must be between 1 and 65535")
            @Max(value = 65535, message = "Port must be between 1 and 65535")
            int port,

            String queue,

            String username,

            String password,

            String virtualHost
    ) {
        public RabbitMQProperties {
            if (queue == null || queue.isBlank()) {
                queue = "test.queue";
            }
            if (username == null || username.isBlank()) {
                username = "guest";
            }
            if (password == null || password.isBlank()) {
                password = "guest";
            }
            if (virtualHost == null || virtualHost.isBlank()) {
                virtualHost = "/";
            }
        }
    }

    /**
     * Настройки Kafka
     */
    public record KafkaProperties(
            @NotBlank(message = "Kafka bootstrap servers cannot be blank")
            String bootstrapServers,

            String topic,

            String groupId,

            @Min(value = 1, message = "Session timeout must be at least 1ms")
            int sessionTimeoutMs,

            @Min(value = 1, message = "Auto commit interval must be at least 1ms")
            int autoCommitIntervalMs
    ) {
        public KafkaProperties {
            if (topic == null || topic.isBlank()) {
                topic = "test-topic";
            }
            if (groupId == null || groupId.isBlank()) {
                groupId = "test-group";
            }
            if (sessionTimeoutMs == 0) {
                sessionTimeoutMs = 10000;
            }
            if (autoCommitIntervalMs == 0) {
                autoCommitIntervalMs = 1000;
            }
        }
    }

    /**
     * Настройки WebSocket
     */
    public record WebSocketProperties(
            String endpoint,

            @Min(value = 1, message = "Port must be between 1 and 65535")
            @Max(value = 65535, message = "Port must be between 1 and 65535")
            int port,

            String path,

            boolean allowedOrigins
    ) {
        public WebSocketProperties {
            if (endpoint == null || endpoint.isBlank()) {
                endpoint = "localhost";
            }
            if (port == 0) {
                port = 8080;
            }
            if (path == null || path.isBlank()) {
                path = "/websocket";
            }
        }
    }

    /**
     * Общие настройки для всех брокеров
     */
    public record CommonProperties(
            @Min(value = 1, message = "Retry attempts must be at least 1")
            int retryAttempts,

            @Min(value = 1, message = "Retry delay must be at least 1ms")
            int retryDelayMs,

            @Min(value = 1, message = "Max message size must be at least 1 byte")
            int maxMessageSizeBytes,

            boolean enableCompression,

            String defaultEncoding
    ) {
        public CommonProperties {
            if (retryAttempts == 0) retryAttempts = 3;
            if (retryDelayMs == 0) retryDelayMs = 1000;
            if (maxMessageSizeBytes == 0) maxMessageSizeBytes = 1024 * 1024;
            if (defaultEncoding == null) defaultEncoding = "UTF-8";
        }
    }

    /**
     * Получить настройки для конкретного брокера по типу
     * Возвращает Optional - может быть пустым если брокер не настроен
     */
    public Optional<Object> getPropertiesForBroker(com.vyanckus.dto.BrokersType brokerType) {
        Object properties = switch (brokerType) {
            case ACTIVEMQ -> activemq;
            case RABBITMQ -> rabbitmq;
            case KAFKA -> kafka;
            case WEBSOCKET -> websocket;
        };
        return Optional.ofNullable(properties);
    }

    /**
     * Получить общие настройки или дефолтные
     */
    public CommonProperties getCommonProperties() {
        return common != null ? common : new CommonProperties(3, 1000, 1024 * 1024, false, "UTF-8");
    }
}
