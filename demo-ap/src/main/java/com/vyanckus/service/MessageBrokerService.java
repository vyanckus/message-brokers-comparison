package com.vyanckus.service;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.config.BrokerProperties;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.factory.MessageBrokerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Основной сервис для работы со всеми брокерами сообщений.
 * Управляет подключением, отправкой и получением сообщений через все доступные брокеры.
 *
 * <p>Предоставляет унифицированный интерфейс для:</p>
 * <ul>
 *   <li>Управления подключениями ко всем брокерам</li>
 *   <li>Отправки сообщений через любой брокер</li>
 *   <li>Подписки на получение сообщений</li>
 *   <li>Мониторинга статуса брокеров</li>
 * </ul>
 */
@Service
public class MessageBrokerService {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerService.class);

    private final MessageBrokerFactory brokerFactory;

    /**
     * Map всех созданных брокеров.
     */
    private final Map<BrokersType, MessageBroker> brokers = new ConcurrentHashMap<>();

    /**
     * Список слушателей для полученных сообщений.
     */
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();

    /**
     * Флаг инициализации сервиса.
     */
    private boolean initialized = false;

    public MessageBrokerService(MessageBrokerFactory brokerFactory) {
        this.brokerFactory = brokerFactory;
        log.info("MessageBrokerService initialized");
    }

    /**
     * Инициализирует все брокеры сообщений и устанавливает подключения.
     * Должен быть вызван перед использованием сервиса.
     *
     * @throws BrokerConnectionException если не удалось подключиться к одному из брокеров
     */
    public void initialize() throws BrokerConnectionException {
        if (initialized) {
            log.debug("MessageBrokerService already initialized");
            return;
        }

        try {
            log.info("Initializing all message brokers...");

            brokers.putAll(brokerFactory.createAllBrokers());

            connectToAllBrokers();

            initialized = true;
            log.info("MessageBrokerService successfully initialized. Active brokers: {}", brokers.size());

        } catch (Exception e) {
            String errorMessage = "Failed to initialize MessageBrokerService";
            log.error("{}: {}", errorMessage, e.getMessage(), e);
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Подключается ко всем созданным брокерам.
     */
    private void connectToAllBrokers() {
        for (MessageBroker broker : brokers.values()) {
            connectToBroker(broker);
        }
    }

    /**
     * Подключается к одному брокеру с обработкой ошибок.
     *
     * @param broker брокер для подключения
     */
    private void connectToBroker(MessageBroker broker) {
        try {
            broker.connect();
            log.info("Successfully connected to {} broker", broker.getBrokerType());
        } catch (BrokerConnectionException e) {
            log.warn("Failed to connect to {} broker: {}", broker.getBrokerType(), e.getMessage());
            // Продолжаем инициализацию других брокеров
        }
    }

    /**
     * Отправляет сообщение через указанный брокер.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!initialized) {
            throw new MessageSendException("MessageBrokerService not initialized. Call initialize() first.");
        }

        MessageBroker broker = brokers.get(request.brokerType());
        if (broker == null) {
            throw new MessageSendException("Broker not available: " + request.brokerType());
        }

        if (!broker.isConnected()) {
            throw new MessageSendException("Broker not connected: " + request.brokerType());
        }

        try {
            log.debug("Sending message via {} broker to: {}", request.brokerType(), request.destination());

            MessageResponse response = broker.sendMessage(request);

            log.debug("Message sent successfully via {} broker. Message ID: {}",
                    request.brokerType(), response.messageId());

            return response;

        } catch (MessageSendException e) {
            log.error("Failed to send message via {} broker: {}", request.brokerType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Подписывается на получение сообщений из указанного брокера.
     *
     * @param brokerType тип брокера для подписки
     * @param destination назначение (очередь, топик)
     */
    public void subscribe(BrokersType brokerType, String destination) {
        if (!initialized) {
            throw new IllegalStateException("MessageBrokerService not initialized. Call initialize() first.");
        }

        MessageBroker broker = brokers.get(brokerType);
        if (broker == null) {
            throw new IllegalArgumentException("Broker not available: " + brokerType);
        }

        try {
            broker.subscribe(destination, this::handleReceivedMessage);
            log.info("Subscribed to {} via {} broker", destination, brokerType);

        } catch (Exception e) {
            log.error("Failed to subscribe to {} via {} broker: {}",
                    destination, brokerType, e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает полученное сообщение от любого брокера.
     * Уведомляет всех зарегистрированных слушателей.
     *
     * @param message полученное сообщение
     * @param destination назначение сообщения
     */
    private void handleReceivedMessage(ReceivedMessage message, String destination) {
        log.debug("Received message from {} broker: {}", message.brokerType(), message.message());

        // Уведомляем всех слушателей
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessage(message, destination);
            } catch (Exception e) {
                log.error("Error in message listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Регистрирует слушатель для получения сообщений.
     *
     * @param listener слушатель сообщений
     */
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
        log.debug("Message listener registered. Total listeners: {}", messageListeners.size());
    }

    /**
     * Удаляет слушатель сообщений.
     *
     * @param listener слушатель для удаления
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
        log.debug("Message listener removed. Total listeners: {}", messageListeners.size());
    }

    /**
     * Возвращает статус всех брокеров.
     *
     * @return Map со статусом подключения каждого брокера
     */
    public Map<BrokersType, Boolean> getBrokersStatus() {
        Map<BrokersType, Boolean> status = new ConcurrentHashMap<>();

        for (Map.Entry<BrokersType, MessageBroker> entry : brokers.entrySet()) {
            status.put(entry.getKey(), entry.getValue().isConnected());
        }

        return status;
    }

    /**
     * Возвращает информацию о здоровье всех брокеров.
     *
     * @return Map с информацией о здоровье каждого брокера
     */
    public Map<BrokersType, Boolean> getBrokersHealth() {
        Map<BrokersType, Boolean> health = new ConcurrentHashMap<>();

        for (Map.Entry<BrokersType, MessageBroker> entry : brokers.entrySet()) {
            health.put(entry.getKey(), entry.getValue().isHealthy());
        }

        return health;
    }

    /**
     * Отключает все брокеры и освобождает ресурсы.
     */
    public void shutdown() {
        log.info("Shutting down MessageBrokerService...");

        for (MessageBroker broker : brokers.values()) {
            try {
                broker.disconnect();
                log.debug("Disconnected from {} broker", broker.getBrokerType());
            } catch (Exception e) {
                log.warn("Error disconnecting from {} broker: {}", broker.getBrokerType(), e.getMessage());
            }
        }

        brokers.clear();
        messageListeners.clear();
        initialized = false;

        log.info("MessageBrokerService shutdown completed");
    }

    /**
     * Проверяет инициализирован ли сервис.
     *
     * @return true если сервис инициализирован, иначе false
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Интерфейс слушателя сообщений для внешних потребителей.
     */
    public interface MessageListener {
        void onMessage(ReceivedMessage message, String destination);
    }
}
