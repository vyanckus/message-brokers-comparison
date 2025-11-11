package com.vyanckus.activemq;

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
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реализация {@link MessageBroker} для Apache ActiveMQ.
 * Использует JMS API для отправки и получения сообщений.
 * Оптимизирован для максимальной производительности в демо.
 *
 * <p><b>Особенности реализации:</b>
 * <ul>
 *   <li>Упрощенная отправка сообщений для производительности</li>
 *   <li>TextMessage-only для безопасности</li>
 *   <li>Автоматическое подтверждение сообщений</li>
 *   <li>Trusted packages ограничение для security</li>
 * </ul>
 *
 * @see MessageBroker
 * @see BrokersType#ACTIVEMQ
 */
@Component
public class ActiveMQBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(ActiveMQBroker.class);

    /**
     * Конфигурация ActiveMQ из application.yml.
     */
    private final BrokerProperties.ActiveMQProperties config;

    /**
     * JMS соединение с ActiveMQ брокером.
     */
    private Connection connection;

    /**
     * JMS сессия для работы с сообщениями.
     */
    private Session session;

    /**
     * Кэшированные MessageProducer'ы управляются вручную в disconnect().
     * Не используем try-with-resources, так как producers переиспользуются.
     */
    private final ConcurrentMap<String, MessageProducer> producerCache = new ConcurrentHashMap<>();

    /**
     * Активные подписки для управления consumer'ами.
     */
    private final ConcurrentMap<String, MessageConsumer> activeConsumers = new ConcurrentHashMap<>();

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerProperties конфигурация всех брокеров из application.yml
     */
    public ActiveMQBroker(BrokerProperties brokerProperties) {
        this.config = Optional.ofNullable(brokerProperties.activemq())
                .orElse(new BrokerProperties.ActiveMQProperties(
                        "tcp://localhost:61616",
                        "test.queue",
                        "admin",
                        "admin",
                        5000
                ));
        log.info("ActiveMQ broker initialized for URL: {}", config.url());
    }

    /**
     * Устанавливает соединение с ActiveMQ брокером.
     *
     * @throws BrokerConnectionException если не удалось установить соединение
     */
    @Override
    public void connect() throws BrokerConnectionException {
        if (isConnected()) {
            log.debug("Already connected to ActiveMQ");
            return;
        }

        try {
            log.info("Connecting to ActiveMQ at: {}", config.url());

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.url());
            connectionFactory.setUserName(config.username());
            connectionFactory.setPassword(config.password());

            // Security settings
            connectionFactory.setTrustAllPackages(false);
            connectionFactory.setTrustedPackages(new ArrayList<>());

            // Настройки для производительности
            connectionFactory.setUseAsyncSend(true);
            connectionFactory.setAlwaysSessionAsync(true);
            connectionFactory.setUseCompression(false); // Без сжатия для скорости

            connection = connectionFactory.createConnection();
            connection.start();

            // Session с большим prefetch размером
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            log.info("Successfully connected to ActiveMQ");

        } catch (JMSException e) {
            String errorMessage = "Failed to connect to ActiveMQ at " + config.url();
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанную очередь ActiveMQ.
     * Использует упрощенную логику для максимальной производительности.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    @Override
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!isConnected()) {
            throw new MessageSendException("Not connected to ActiveMQ.");
        }

        try {
            String destinationName = request.destination() != null ? request.destination() : config.queue();

            // Кэшированный Producer
            MessageProducer producer = producerCache.computeIfAbsent(destinationName, queue -> {
                try {
                    Destination destination = session.createQueue(queue);
                    MessageProducer p = session.createProducer(destination);

                    // Настройки Producer для скорости
                    p.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    p.setTimeToLive(10000);

                    return p;
                } catch (JMSException e) {
                    throw new MessageSendException("Failed to create producer for queue: " + queue, e);
                }
            });

            // Быстрое создание и отправка сообщения
            TextMessage message = session.createTextMessage();
            message.setText(request.message());
            producer.send(message);

            return MessageResponse.success(BrokersType.ACTIVEMQ, "activemq-" + System.nanoTime());

        } catch (MessageSendException e) {
            throw e;
        } catch (JMSException e) {
            throw new MessageSendException("Failed to send message to ActiveMQ", e);
        } catch (Exception e) {
            throw new MessageSendException("Unexpected error while sending message", e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанной очереди ActiveMQ.
     *
     * @param destination очередь для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если не удалось подписаться на очередь
     */
    @Override
    public void subscribe(String destination, MessageListener listener) throws SubscriptionException {
        if (!isConnected()) {
            throw new SubscriptionException("Not connected to ActiveMQ. Call connect() first.");
        }

        try {
            Destination jmsDestination = session.createQueue(destination);
            MessageConsumer consumer = session.createConsumer(jmsDestination);

            consumer.setMessageListener(jmsMessage ->
                    handleIncomingMessage(jmsMessage, destination, listener));

            activeConsumers.put(destination, consumer);

            log.info("Successfully subscribed to queue: {}", destination);

        } catch (JMSException e) {
            String errorMessage = "Failed to subscribe to queue " + destination;
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Обрабатывает входящее сообщение ActiveMQ.
     *
     * @param jmsMessage входящее сообщение
     * @param destination очередь-источник
     * @param listener обработчик сообщений
     */
    private void handleIncomingMessage(Message jmsMessage, String destination, MessageListener listener) {
        try {
            if (jmsMessage instanceof TextMessage textMessage) {
                ReceivedMessage receivedMessage = new ReceivedMessage(
                        BrokersType.ACTIVEMQ,
                        destination,
                        textMessage.getText(),
                        "activemq-recv-" + System.nanoTime(),
                        java.time.Instant.now(),
                        java.util.Map.of()
                );
                listener.onMessage(receivedMessage, destination);
            }
        } catch (JMSException e) {
            log.error("Error processing message from queue {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Отписывается от получения сообщений из указанной очереди ActiveMQ.
     *
     * @param destination очередь от которой отписываемся
     * @throws SubscriptionException если не удалось отписаться
     */
    @Override
    public void unsubscribe(String destination) throws SubscriptionException {
        MessageConsumer consumer = activeConsumers.remove(destination);
        if (consumer != null) {
            try {
                consumer.close();
                log.info("Unsubscribed from: {}", destination);
            } catch (JMSException e) {
                log.warn("Error unsubscribing from {}: {}", destination, e.getMessage());
            }
        }
    }

    /**
     * Отписывается от всех активных подписок ActiveMQ.
     */
    @Override
    public void unsubscribeAll() throws SubscriptionException {
        activeConsumers.keySet().forEach(queue -> {
            try {
                unsubscribe(queue);
            } catch (SubscriptionException e) {
                log.warn("Failed to unsubscribe from {}: {}", queue, e.getMessage());
            }
        });
    }

    /**
     * Закрывает соединение с ActiveMQ и освобождает ресурсы.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from ActiveMQ...");

        // Закрываем кэшированные producers
        producerCache.values().forEach(producer -> {
            try {
                producer.close();
            } catch (JMSException e) {
                log.debug("Error closing producer: {}", e.getMessage());
            }
        });
        producerCache.clear();

        try {
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            log.debug("Error closing resources: {}", e.getMessage());
        }

        session = null;
        connection = null;
        log.info("Disconnected from ActiveMQ");
    }

    /**
     * Проверяет наличие активного соединения с ActiveMQ.
     */
    @Override
    public boolean isConnected() {
        return session != null;
    }

    /**
     * Возвращает тип брокера - ActiveMQ.
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.ACTIVEMQ;
    }

    /**
     * Проверяет работоспособность брокера.
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }
}
