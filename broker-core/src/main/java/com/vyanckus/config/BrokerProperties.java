package com.vyanckus.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

/**
 * Центральный класс конфигурационных свойств для всех брокеров сообщений.
 * Содержит типобезопасные настройки для каждого поддерживаемого брокера.
 *
 * <p><b>Особенности дизайна:</b>
 * <ul>
 *   <li>Использует Java records для иммутабельности и компактности</li>
 *   <li>Вложенные records для каждого типа брокера</li>
 *   <li>Умные значения по умолчанию через компактные конструкторы</li>
 *   <li>Валидация конфигурации через Jakarta Bean Validation</li>
 * </ul>
 *
 * <p><b>Структура конфигурации в application.yml:</b>
 * <pre>
 * message:
 *   broker:
 *     activemq:
 *       url: tcp://localhost:61616
 *       queue: test.queue
 *     rabbitmq:
 *       host: localhost
 *       port: 5672
 *     kafka:
 *       bootstrap-servers: localhost:9092
 *     websocket:
 *       endpoint: localhost
 *       port: 8080
 *     common:
 *       retry-attempts: 3
 * </pre>
 *
 * @param activemq настройки для Apache ActiveMQ
 * @param rabbitmq настройки для RabbitMQ
 * @param kafka настройки для Apache Kafka
 * @param websocket настройки для WebSocket соединений
 * @param common общие настройки для всех брокеров
 */
@ConfigurationProperties(prefix = "message.broker")
@Validated
public record BrokerProperties(

        @Valid
        ActiveMQProperties activemq,

        @Valid
        RabbitMQProperties rabbitmq,

        @Valid
        KafkaProperties kafka,

        @Valid
        WebSocketProperties websocket,

        @Valid
        CommonProperties common

) {

    /**
     * Проверяет, настроен ли указанный брокер в конфигурации.
     * Брокер считается настроенным, если соответствующий раздел конфигурации присутствует.
     *
     * @param brokerType тип брокера для проверки
     * @return true если брокер присутствует в конфигурации, иначе false
     */
    public boolean isBrokerConfigured(com.vyanckus.dto.BrokersType brokerType) {
        return switch (brokerType) {
            case ACTIVEMQ -> activemq != null;
            case RABBITMQ -> rabbitmq != null;
            case KAFKA -> kafka != null;
            case WEBSOCKET -> websocket != null;
        };
    }

    /**
     * Настройки для Apache ActiveMQ брокера.
     *
     * @param url URL для подключения к ActiveMQ (обязательно)
     * @param queue имя очереди по умолчанию
     * @param username имя пользователя
     * @param password пароль
     * @param connectionTimeoutMs таймаут подключения в миллисекундах
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
     * Настройки для RabbitMQ брокера.
     *
     * @param host хост RabbitMQ (обязательно)
     * @param port порт RabbitMQ
     * @param queue имя очереди по умолчанию
     * @param username имя пользователя
     * @param password пароль
     * @param virtualHost виртуальный хост
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
     * Настройки для Apache Kafka брокера.
     *
     * @param bootstrapServers список bootstrap серверов (обязательно)
     * @param topic имя топика по умолчанию
     * @param groupId идентификатор consumer group
     * @param sessionTimeoutMs таймаут сессии в миллисекундах
     * @param autoCommitIntervalMs интервал авто-коммита в миллисекундах
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
     * Настройки для WebSocket соединений.
     *
     * @param endpoint endpoint WebSocket сервера
     * @param port порт WebSocket сервера
     * @param path путь WebSocket endpoint
     * @param allowedOrigins разрешить все origins
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
     * Общие настройки, применяемые ко всем брокерам.
     *
     * @param retryAttempts количество попыток повтора при ошибках
     * @param retryDelayMs задержка между попытками в миллисекундах
     * @param maxMessageSizeBytes максимальный размер сообщения в байтах
     * @param enableCompression включить сжатие сообщений
     * @param defaultEncoding кодировка по умолчанию
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
     * Получить настройки для конкретного брокера по типу.
     *
     * @param brokerType тип брокера
     * @return Optional с настройками брокера или empty если брокер не настроен
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
     * Получить общие настройки для всех брокеров.
     * Если общие настройки не указаны в конфигурации, возвращаются значения по умолчанию.
     *
     * @return общие настройки (никогда не null)
     */
    public CommonProperties getCommonProperties() {
        return common != null ? common : new CommonProperties(3, 1000, 1024 * 1024, false, "UTF-8");
    }
}
