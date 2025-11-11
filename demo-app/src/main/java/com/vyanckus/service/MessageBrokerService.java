package com.vyanckus.service;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.broker.MessageListener;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.factory.MessageBrokerFactory;
import com.vyanckus.metrics.BrokerMetrics;
import io.micrometer.core.instrument.Timer;
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
 * <p><b>Предоставляет унифицированный интерфейс для:</b>
 * <ul>
 *   <li>Управления подключениями ко всем брокерам</li>
 *   <li>Отправки сообщений через любой брокер</li>
 *   <li>Подписки и отписки от получения сообщений</li>
 *   <li>Мониторинга статуса и здоровья брокеров</li>
 *   <li>Управления слушателями входящих сообщений</li>
 * </ul>
 *
 * <p><b>Типичный lifecycle использования:</b>
 * <ol>
 *   <li>{@link #initialize()} - инициализация всех брокеров</li>
 *   <li>{@link #subscribe(BrokersType, String)} - подписка на сообщения</li>
 *   <li>{@link #sendMessage(MessageRequest)} - отправка сообщений</li>
 *   <li>{@link #unsubscribe(BrokersType, String)} - отписка когда не нужно</li>
 *   <li>{@link #shutdown()} - корректное завершение работы</li>
 * </ol>
 *
 * @see MessageBroker
 * @see MessageListener
 * @see BrokersType
 */
@Service
public class MessageBrokerService {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerService.class);

    private final MessageBrokerFactory brokerFactory;
    private final BrokerMetrics brokerMetrics;

    /**
     * Map всех созданных брокеров.
     * Ключ: тип брокера, Значение: экземпляр брокера
     */
    private final Map<BrokersType, MessageBroker> brokers = new ConcurrentHashMap<>();

    /**
     * Список слушателей для полученных сообщений.
     * Использует CopyOnWriteArrayList для thread-safe операций.
     */
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();

    /**
     * Флаг инициализации сервиса.
     * Защищает от операций до полной инициализации брокеров.
     */
    private boolean initialized = false;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerFactory фабрика для создания брокеров
     * @param brokerMetrics метрики для мониторинга производительности
     */
    public MessageBrokerService(MessageBrokerFactory brokerFactory, BrokerMetrics brokerMetrics) {
        this.brokerFactory = brokerFactory;
        this.brokerMetrics = brokerMetrics;
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
     * Обрабатывает ошибки подключения отдельных брокеров без прерывания инициализации.
     */
    private void connectToAllBrokers() {
        for (MessageBroker broker : brokers.values()) {
            connectToBroker(broker);
        }
    }

    /**
     * Подключается к одному брокеру с обработкой ошибок.
     * Продолжает инициализацию других брокеров при неудаче.
     *
     * @param broker брокер для подключения
     */
    private void connectToBroker(MessageBroker broker) {
        try {
            broker.connect();
            log.info("Successfully connected to {} broker", broker.getBrokerType());
        } catch (BrokerConnectionException e) {
            log.warn("Failed to connect to {} broker: {}", broker.getBrokerType(), e.getMessage());
        }
    }

    /**
     * Отправляет сообщение через указанный брокер.
     * Включает метрики производительности и обработку ошибок.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     * @throws IllegalStateException если сервис не инициализирован
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

        Timer.Sample timer = brokerMetrics.startMessageTimer();

        try {
            log.debug("Sending message via {} broker to: {}", request.brokerType(), request.destination());

            MessageResponse response = broker.sendMessage(request);
            brokerMetrics.incrementSentMessages(request.brokerType());

            log.debug("Message sent successfully via {} broker. Message ID: {}",
                    request.brokerType(), response.messageId());

            return response;

        } catch (MessageSendException e) {
            log.error("Failed to send message via {} broker: {}", request.brokerType(), e.getMessage(), e);
            throw e;
        } finally {
            brokerMetrics.stopMessageTimer(timer, request.brokerType());
        }
    }

    /**
     * Подписывается на получение сообщений из указанного брокера.
     * Регистрирует внутренний обработчик для маршрутизации сообщений всем слушателям.
     *
     * @param brokerType тип брокера для подписки
     * @param destination назначение (очередь, топик)
     * @throws IllegalStateException если сервис не инициализирован
     * @throws IllegalArgumentException если брокер недоступен
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
     * Отписывается от получения сообщений из указанного брокера.
     * Освобождает ресурсы брокера, связанные с подпиской.
     *
     * @param brokerType тип брокера для отписки
     * @param destination назначение (очередь, топик)
     * @throws IllegalStateException если сервис не инициализирован
     * @throws IllegalArgumentException если брокер недоступен
     */
    public void unsubscribe(BrokersType brokerType, String destination) {
        if (!initialized) {
            throw new IllegalStateException("MessageBrokerService not initialized. Call initialize() first.");
        }

        MessageBroker broker = brokers.get(brokerType);
        if (broker == null) {
            throw new IllegalArgumentException("Broker not available: " + brokerType);
        }

        try {
            broker.unsubscribe(destination);
            log.info("Unsubscribed from {} via {} broker", destination, brokerType);
        } catch (Exception e) {
            log.error("Failed to unsubscribe from {} via {} broker: {}",
                    destination, brokerType, e.getMessage(), e);
            throw new RuntimeException("Unsubscribe failed", e);
        }
    }

    /**
     * Отписывается от всех подписок всех брокеров.
     * Используется при shutdown для корректного освобождения ресурсов.
     *
     * @throws IllegalStateException если сервис не инициализирован
     */
    public void unsubscribeAll() {
        if (!initialized) {
            log.debug("Service not initialized, no subscriptions to unsubscribe from");
            return;
        }

        log.info("Unsubscribing from all brokers...");

        for (MessageBroker broker : brokers.values()) {
            try {
                broker.unsubscribeAll();
                log.debug("Unsubscribed from all destinations via {} broker", broker.getBrokerType());
            } catch (Exception e) {
                log.warn("Error unsubscribing from all destinations for {} broker: {}",
                        broker.getBrokerType(), e.getMessage());
            }
        }

        log.info("Unsubscribed from all brokers");
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
        brokerMetrics.incrementReceivedMessages(message.brokerType());

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
     * Слушатель будет получать все входящие сообщения от всех брокеров.
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
     * Здоровье включает проверку соединения и базовой функциональности.
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
     * Получить конкретный брокер по типу.
     *
     * @param brokerType тип брокера
     * @return экземпляр брокера или null если не найден
     */
    public MessageBroker getBroker(BrokersType brokerType) {
        return brokers.get(brokerType);
    }

    /**
     * Отключает все брокеры и освобождает ресурсы.
     * Автоматически отписывается от всех активных подписок.
     * Должен вызываться при завершении работы приложения.
     */
    public void shutdown() {
        log.info("Shutting down MessageBrokerService...");

        // Сначала отписываемся от всех подписок
        unsubscribeAll();

        // Затем отключаем брокеры
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
}
