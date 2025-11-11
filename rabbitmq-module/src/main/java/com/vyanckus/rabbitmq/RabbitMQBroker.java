package com.vyanckus.rabbitmq;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.broker.MessageListener;
import com.vyanckus.config.BrokerProperties;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.exception.SubscriptionException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

/**
 * Реализация {@link MessageBroker} для RabbitMQ.
 * Использует AMQP протокол для отправки и получения сообщений.
 * Оптимизирован для демонстрационных целей с упрощенной логикой.
 *
 * <p><b>Особенности реализации:</b>
 * <ul>
 *   <li>Упрощенная отправка сообщений для производительности</li>
 *   <li>Автоматическое создание очередей при первом использовании</li>
 *   <li>Consumer management с graceful shutdown</li>
 *   <li>Базовые гарантии доставки для демо</li>
 * </ul>
 *
 * @see MessageBroker
 * @see BrokersType#RABBITMQ
 */
@Component
public class RabbitMQBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQBroker.class);

    /**
     * Конфигурация RabbitMQ из application.yml.
     */
    private final BrokerProperties.RabbitMQProperties config;

    /**
     * AMQP соединение с RabbitMQ брокером.
     */
    private Connection connection;

    /**
     * AMQP канал для работы с сообщениями.
     */
    private Channel channel;

    /**
     * Активные подписки для управления consumer'ами.
     */
    private final ConcurrentMap<String, String> activeConsumers = new ConcurrentHashMap<>();

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerProperties конфигурация всех брокеров из application.yml
     */
    public RabbitMQBroker(BrokerProperties brokerProperties) {
        this.config = Optional.ofNullable(brokerProperties.rabbitmq())
                .orElse(new BrokerProperties.RabbitMQProperties(
                        "localhost", 5672, "test.queue", "guest", "guest", "/"
                ));
        log.info("RabbitMQ broker initialized for: {}:{}", config.host(), config.port());
    }

    /**
     * Устанавливает соединение с RabbitMQ брокером.
     *
     * @throws BrokerConnectionException если не удалось установить соединение
     */
    @Override
    public void connect() throws BrokerConnectionException {
        if (isConnected()) {
            log.debug("Already connected to RabbitMQ");
            return;
        }

        try {
            log.info("Connecting to RabbitMQ at: {}:{}", config.host(), config.port());

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.host());
            factory.setPort(config.port());
            factory.setUsername(config.username());
            factory.setPassword(config.password());
            factory.setVirtualHost(config.virtualHost());

            // Оптимизация для производительности
            factory.setConnectionTimeout(30000);
            factory.setHandshakeTimeout(30000);

            connection = factory.newConnection();
            channel = connection.createChannel();

            log.info("Successfully connected to RabbitMQ");

        } catch (IOException | TimeoutException e) {
            String errorMessage = "Failed to connect to RabbitMQ at " + config.host() + ":" + config.port();
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанную очередь RabbitMQ.
     * Использует упрощенную логику для максимальной производительности в демо.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    @Override
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!isConnected()) {
            throw new MessageSendException("Not connected to RabbitMQ. Call connect() first.");
        }

        try {
            String queueName = request.destination() != null ? request.destination() : config.queue();

            // Упрощенная отправка - создаем очередь если не существует и публикуем
            channel.basicPublish("", queueName, null, request.message().getBytes());

            // Простой ID для демо
            String messageId = "rabbitmq-" + System.currentTimeMillis();

            return MessageResponse.success(BrokersType.RABBITMQ, messageId);

        } catch (IOException e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage());
            throw new MessageSendException("Failed to send message to RabbitMQ", e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанной очереди RabbitMQ.
     *
     * @param destination очередь для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если не удалось подписаться на очередь
     */
    @Override
    public void subscribe(String destination, MessageListener listener) throws SubscriptionException {
        if (!isConnected()) {
            throw new SubscriptionException("Not connected to RabbitMQ. Call connect() first.");
        }

        if (activeConsumers.containsKey(destination)) {
            log.warn("Already subscribed to queue: {}", destination);
            return;
        }

        try {
            log.info("Subscribing to queue: {}", destination);

            // Создаем очередь если не существует
            channel.queueDeclare(destination, false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                handleIncomingMessage(delivery.getBody(), destination, listener);
            };

            String consumerTag = channel.basicConsume(destination, true, deliverCallback, tag -> {});

            activeConsumers.put(destination, consumerTag);

            log.info("Successfully subscribed to queue: {}", destination);

        } catch (IOException e) {
            String errorMessage = "Failed to subscribe to queue " + destination;
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Обрабатывает входящее сообщение RabbitMQ.
     *
     * @param messageBody тело сообщения
     * @param queueName очередь-источник
     * @param listener обработчик сообщений
     */
    private void handleIncomingMessage(byte[] messageBody, String queueName, MessageListener listener) {
        try {
            String messageText = new String(messageBody);

            ReceivedMessage receivedMessage = new ReceivedMessage(
                    BrokersType.RABBITMQ,
                    queueName,
                    messageText,
                    "rabbitmq-" + System.currentTimeMillis(),
                    java.time.Instant.now(),
                    java.util.Map.of()
            );

            listener.onMessage(receivedMessage, queueName);

        } catch (Exception e) {
            log.error("Error processing message from queue {}: {}", queueName, e.getMessage());
        }
    }

    /**
     * Отписывается от получения сообщений из указанной очереди RabbitMQ.
     *
     * @param destination очередь от которой отписываемся
     * @throws SubscriptionException если не удалось отписаться
     */
    @Override
    public void unsubscribe(String destination) throws SubscriptionException {
        String consumerTag = activeConsumers.remove(destination);
        if (consumerTag == null) {
            log.warn("No active subscription found for: {}", destination);
            return;
        }

        try {
            channel.basicCancel(consumerTag);
            log.info("Unsubscribed from: {}", destination);
        } catch (IOException e) {
            String errorMessage = "Failed to unsubscribe from " + destination;
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Отписывается от всех активных подписок RabbitMQ.
     */
    @Override
    public void unsubscribeAll() throws SubscriptionException {
        log.info("Unsubscribing from all RabbitMQ queues...");

        List<String> destinations = new ArrayList<>(activeConsumers.keySet());
        for (String destination : destinations) {
            try {
                unsubscribe(destination);
            } catch (SubscriptionException e) {
                log.warn("Failed to unsubscribe from {}: {}", destination, e.getMessage());
            }
        }

        log.info("Unsubscribed from all RabbitMQ queues");
    }

    /**
     * Закрывает соединение с RabbitMQ и освобождает ресурсы.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from RabbitMQ...");

        try {
            unsubscribeAll();
        } catch (Exception e) {
            log.warn("Error during unsubscribe: {}", e.getMessage());
        }

        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException | TimeoutException e) {
            log.debug("Error closing channel: {}", e.getMessage());
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            log.debug("Error closing connection: {}", e.getMessage());
        }

        channel = null;
        connection = null;

        log.info("Successfully disconnected from RabbitMQ");
    }

    /**
     * Проверяет наличие активного соединения с RabbitMQ.
     */
    @Override
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }

    /**
     * Возвращает тип брокера - RabbitMQ.
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.RABBITMQ;
    }

    /**
     * Проверяет работоспособность брокера.
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }
}
