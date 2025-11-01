package com.vyanckus.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Конфигурационные свойства для всех брокеров сообщений.
 * Автоматически связывается с application.yml
 */
@ConfigurationProperties(prefix = "message.broker")
@Validated
public record BrokerProperties(

        /**
         * Настройки для ActiveMQ
         */
        @NotNull(message = "ActiveMQ configuration is required")
        @Valid
        ActiveMQProperties activemq,

        /**
         * Настройки для RabbitMQ
         */
        @NotNull(message = "RabbitMQ configuration is required")
        @Valid
        RabbitMQProperties rabbitmq,

        /**
         * Настройки для Kafka
         */
        @NotNull(message = "Kafka configuration is required")
        @Valid KafkaProperties kafka,

        /**
         * Настройки для WebSocket
         */
        @NotNull(message = "WebSocket configuration is required")
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

            @NotBlank(message = "ActiveMQ queue cannot be blank")
            String queue,

            @NotBlank(message = "ActiveMQ username cannot be blank")
            String username,

            @NotBlank(message = "ActiveMQ password cannot be blank")
            String password,

            @Min(value = 1, message = "Timeout must be at least 1ms")
            int connectionTimeoutMs
    ) {}

    /**
     * Настройки RabbitMQ
     */
    public record RabbitMQProperties(
            @NotBlank(message = "RabbitMQ host cannot be blank")
            String host,

            @Min(value = 1, message = "Port must be between 1 and 65535")
            @Max(value = 65535, message = "Port must be between 1 and 65535")
            int port,

            @NotBlank(message = "RabbitMQ queue cannot be blank")
            String queue,

            @NotBlank(message = "RabbitMQ username cannot be blank")
            String username,

            @NotBlank(message = "RabbitMQ password cannot be blank")
            String password,

            @NotBlank(message = "RabbitMQ virtual host cannot be blank")
            String virtualHost
    ) {}

    /**
     * Настройки Kafka
     */
    public record KafkaProperties(
            @NotBlank(message = "Kafka bootstrap servers cannot be blank")
            String bootstrapServers,

            @NotBlank(message = "Kafka topic cannot be blank")
            String topic,

            String groupId,

            @Min(value = 1, message = "Session timeout must be at least 1ms")
            int sessionTimeoutMs,

            @Min(value = 1, message = "Auto commit interval must be at least 1ms")
            int autoCommitIntervalMs
    ) {}

    /**
     * Настройки WebSocket
     */
    public record WebSocketProperties(
            @NotBlank(message = "WebSocket endpoint cannot be blank")
            String endpoint,

            @Min(value = 1, message = "Port must be between 1 and 65535")
            @Max(value = 65535, message = "Port must be between 1 and 65535")
            int port,

            @NotBlank(message = "WebSocket path cannot be blank")
            String path,

            boolean allowedOrigins
    ) {}

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

            @NotBlank(message = "Default encoding cannot be blank")
            String defaultEncoding
    ) {

        /**
         * Конструктор со значениями по умолчанию
         */
        public CommonProperties {
            if (retryAttempts == 0) retryAttempts = 3;
            if (retryDelayMs == 0) retryDelayMs = 1000;
            if (maxMessageSizeBytes == 0) maxMessageSizeBytes = 1024 * 1024;
            if (defaultEncoding == null) defaultEncoding = "UTF-8";
        }
    }

    /**
     * Получить настройки для конкретного брокера по типу
     */
    public Object getPropertiesForBroker(com.vyanckus.dto.BrokersType brokerType) {
        return switch (brokerType) {
            case ACTIVEMQ -> activemq();
            case RABBITMQ -> rabbitmq();
            case KAFKA -> kafka();
            case WEBSOCKET -> websocket();
        };
    }
}
