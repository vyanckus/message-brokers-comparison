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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

/**
 * Реализация {@link MessageBroker} для RabbitMQ.
 * Использует AMQP протокол для отправки и получения сообщений через RabbitMQ брокер.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Подключение к RabbitMQ брокеру</li>
 *   <li>Отправка сообщений в очереди</li>
 *   <li>Подписка на получение сообщений из очередей</li>
 *   <li>Управление соединением и каналами</li>
 * </ul>
 *
 * @author vyanckus
 * @version 1.0
 * @see MessageBroker
 * @see BrokersType#RABBITMQ
 */
@Component
public class RabbitMQBroker implements MessageBroker {


    /**
     * Логгер для записи событий и ошибок.
     */
    private static final Logger log = LoggerFactory.getLogger(RabbitMQBroker.class);

    /**
     * Константы для шаблонов сообщений
     */
    private static final String LOG_FORMAT = "{}: {}";
    private static final String SEND_ERROR_MSG = "Failed to send message to RabbitMQ";

    /**
     * Конфигурация RabbitMQ из application.yml.
     */
    private final BrokerProperties.RabbitMQProperties config;

    /**
     * Соединение с RabbitMQ брокером.
     */
    private Connection connection;

    /**
     * AMQP канал для работы с сообщениями.
     */
    private Channel channel;

    /**
     * Активные потребители для управления подписками.
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
     * Создает ConnectionFactory, соединение и канал для работы с AMQP.
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

            // Создание фабрики соединений
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.host());
            factory.setPort(config.port());
            factory.setUsername(config.username());
            factory.setPassword(config.password());
            factory.setVirtualHost(config.virtualHost());

            // Установка соединения
            connection = factory.newConnection();

            // Создание канала
            channel = connection.createChannel();

            log.info("Successfully connected to RabbitMQ");

        } catch (IOException | TimeoutException e) {
            String errorMessage = String.format("Failed to connect to RabbitMQ at %s:%d",
                    config.host(), config.port());
            log.error(LOG_FORMAT, errorMessage, e.getMessage(), e);
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанную очередь RabbitMQ.
     * Если очередь не указана в запросе, используется очередь из конфигурации.
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
            // Используем очередь из запроса или конфигурации по умолчанию
            String queueName = request.destination() != null ?
                    request.destination() : config.queue();

            log.debug("Sending message to queue: {}", queueName);

            // Объявляем очередь (создаем если не существует)
            channel.queueDeclare(queueName, false, false, false, null);

            // Отправляем сообщение
            channel.basicPublish("", queueName, null, request.message().getBytes());

            // Генерируем ID сообщения для трейсинга
            String messageId = UUID.randomUUID().toString();

            log.debug("Message successfully sent to queue: {}", queueName);

            return MessageResponse.success(BrokersType.RABBITMQ, messageId);

        } catch (IOException e) {
            log.error(LOG_FORMAT, SEND_ERROR_MSG, e.getMessage(), e);
            throw new MessageSendException(SEND_ERROR_MSG, e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанной очереди.
     * Регистрирует DeliverCallback для асинхронной обработки входящих сообщений.
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

        // Проверяем нет ли уже активной подписки
        if (activeConsumers.containsKey(destination)) {
            log.warn("Already subscribed to queue: {}", destination);
            return;
        }

        try {
            log.info("Subscribing to queue: {}", destination);

            // Объявляем очередь (создаем если не существует)
            channel.queueDeclare(destination, false, false, false, null);

            // Создаем callback для обработки входящих сообщений
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                handleIncomingMessage(delivery.getBody(), destination, listener);
            };

            // Начинаем потребление сообщений
            String consumerTag = channel.basicConsume(destination, true, deliverCallback, tag -> {});

            // Сохраняем consumer tag для управления подпиской
            activeConsumers.put(destination, consumerTag);

            log.info("Successfully subscribed to queue: {}", destination);

        } catch (IOException e) {
            String errorMessage = String.format("Failed to subscribe to queue %s", destination);
            log.error(LOG_FORMAT, errorMessage, e.getMessage(), e);
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Обрабатывает входящее сообщение и преобразует его в унифицированный формат.
     *
     * @param messageBody тело входящего сообщения
     * @param queueName очередь-источник сообщения
     * @param listener обработчик для уведомления о новом сообщении
     */
    private void handleIncomingMessage(byte[] messageBody, String queueName, MessageListener listener) {
        try {
            String messageText = new String(messageBody);

            log.debug("Received message from queue: {} - {}", queueName, messageText);

            // Создаем унифицированное представление сообщения
            ReceivedMessage receivedMessage = new ReceivedMessage(
                    BrokersType.RABBITMQ,
                    queueName,
                    messageText,
                    UUID.randomUUID().toString(), // RabbitMQ не предоставляет message ID по умолчанию
                    java.time.Instant.now(),
                    java.util.Map.of()
            );

            // Уведомляем обработчик
            listener.onMessage(receivedMessage, queueName);

        } catch (Exception e) {
            log.error("Error processing message from queue {}: {}", queueName, e.getMessage(), e);
        }
    }

    /**
     * Закрывает соединение с RabbitMQ брокером и освобождает ресурсы.
     * Ресурсы закрываются в правильном порядке: канал → соединение.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from RabbitMQ...");

        try {
            cancelAllSubscriptions();

            if (channel != null && channel.isOpen()) {
                channel.close();
                channel = null;
                log.debug("AMQP channel closed");
            }

            if (connection != null && connection.isOpen()) {
                connection.close();
                connection = null;
                log.debug("AMQP connection closed");
            }

            log.info("Successfully disconnected from RabbitMQ");

        } catch (IOException | TimeoutException e) {
            log.warn("Error during disconnect from RabbitMQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Отменяет все активные подписки на очереди.
     */
    private void cancelAllSubscriptions() {
        for (String consumerTag : activeConsumers.values()) {
            cancelSubscription(consumerTag);
        }
        activeConsumers.clear();
    }

    /**
     * Отменяет одну подписку по consumer tag.
     */
    private void cancelSubscription(String consumerTag) {
        try {
            channel.basicCancel(consumerTag);
            log.debug("Canceled consumer: {}", consumerTag);
        } catch (IOException e) {
            log.debug("Error canceling consumer {}: {}", consumerTag, e.getMessage());
        }
    }

    /**
     * Проверяет наличие активного соединения с RabbitMQ.
     *
     * @return true если соединение установлено, иначе false
     */
    @Override
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }

    /**
     * Возвращает тип брокера - RabbitMQ.
     *
     * @return тип брокера {@link BrokersType#RABBITMQ}
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.RABBITMQ;
    }

    /**
     * Проверяет работоспособность брокера.
     * В упрощенной реализации проверяет только наличие соединения.
     *
     * @return true если брокер работает корректно, иначе false
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }
}
