package com.vyanckus.websocket;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реализация {@link MessageBroker} для WebSocket соединений.
 * Использует STOMP over WebSocket для реального времени двусторонней связи.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Отправка сообщений через WebSocket</li>
 *   <li>Подписка на получение сообщений по темам (topics)</li>
 *   <li>Управление WebSocket соединениями</li>
 *   <li>Интеграция с Spring STOMP</li>
 * </ul>
 *
 * @author vyanckus
 * @version 1.0
 * @see MessageBroker
 * @see BrokersType#WEBSOCKET
 */
@Component
public class WebSocketBroker implements MessageBroker {

    /**
     * Логгер для записи событий и ошибок.
     */
    private static final Logger log = LoggerFactory.getLogger(WebSocketBroker.class);

    /**
     * Константы для шаблонов сообщений.
     */
    private static final String LOG_FORMAT = "{}: {}";
    private static final String SEND_ERROR_MSG = "Failed to send message via WebSocket";

    /**
     * Конфигурация WebSocket из application.yml.
     */
    private final BrokerProperties.WebSocketProperties config;

    /**
     * Spring Messaging Template для отправки сообщений через WebSocket.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Активные подписки на WebSocket темы.
     */
    private final ConcurrentMap<String, MessageListener> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * Флаг подключения (WebSocket всегда "подключен" после инициализации Spring).
     */
    private boolean connected = false;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerProperties конфигурация всех брокеров из application.yml
     * @param messagingTemplate Spring Messaging Template для WebSocket
     */
    public WebSocketBroker(BrokerProperties brokerProperties, SimpMessagingTemplate messagingTemplate) {
        this.config = brokerProperties.websocket();
        this.messagingTemplate = messagingTemplate;
        log.info("WebSocket broker initialized for endpoint: {}:{}", config.endpoint(), config.port());
    }

    /**
     * "Подключает" WebSocket брокер.
     * В случае WebSocket подключение устанавливается автоматически Spring'ом.
     * Этот метод просто устанавливает флаг connected.
     *
     * @throws BrokerConnectionException если WebSocket не настроен корректно
     */
    @Override
    public void connect() throws BrokerConnectionException {
        if (isConnected()) {
            log.debug("WebSocket broker already connected");
            return;
        }

        try {
            log.info("Initializing WebSocket broker");

            // WebSocket подключение управляется Spring'ом автоматически
            // Проверяем только что messagingTemplate доступен
            if (messagingTemplate == null) {
                throw new IllegalStateException("WebSocket messaging template is not available");
            }

            connected = true;
            log.info("WebSocket broker successfully initialized");

        } catch (Exception e) {
            String errorMessage = "Failed to initialize WebSocket broker";
            log.error(LOG_FORMAT, errorMessage, e.getMessage(), e);
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение через WebSocket на указанный destination.
     * Сообщение отправляется всем подписанным клиентам.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    @Override
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!isConnected()) {
            throw new MessageSendException("WebSocket broker not connected. Call connect() first.");
        }

        try {
            // Используем destination из запроса или конфигурации по умолчанию
            String destination = request.destination() != null ?
                    request.destination() : config.path();

            log.debug("Sending WebSocket message to destination: {}", destination);

            // Отправляем сообщение через WebSocket
            messagingTemplate.convertAndSend(destination, request.message());

            // Генерируем ID сообщения для трейсинга
            String messageId = UUID.randomUUID().toString();

            log.debug("WebSocket message successfully sent to destination: {}", destination);

            // Уведомляем локальных подписчиков об отправленном сообщении
            notifyLocalSubscribers(destination, request.message(), messageId);

            return MessageResponse.success(BrokersType.WEBSOCKET, messageId);

        } catch (Exception e) {
            log.error(LOG_FORMAT, SEND_ERROR_MSG, e.getMessage(), e);
            throw new MessageSendException(SEND_ERROR_MSG, e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанного destination.
     * Регистрирует MessageListener для локальной обработки входящих сообщений.
     *
     * @param destination тема для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если не удалось подписаться
     */
    @Override
    public void subscribe(String destination, MessageListener listener) throws SubscriptionException {
        if (!isConnected()) {
            throw new SubscriptionException("WebSocket broker not connected. Call connect() first.");
        }

        try {
            log.info("Subscribing to WebSocket destination: {}", destination);

            // Регистрируем подписку локально
            activeSubscriptions.put(destination, listener);

            log.info("Successfully subscribed to WebSocket destination: {}", destination);

        } catch (Exception e) {
            String errorMessage = String.format("Failed to subscribe to WebSocket destination %s", destination);
            log.error(LOG_FORMAT, errorMessage, e.getMessage(), e);
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Уведомляет локальных подписчиков о новом сообщении.
     * Используется для симуляции получения сообщений через WebSocket.
     *
     * @param destination тема сообщения
     * @param message текст сообщения
     * @param messageId идентификатор сообщения
     */
    private void notifyLocalSubscribers(String destination, String message, String messageId) {
        MessageListener listener = activeSubscriptions.get(destination);
        if (listener != null) {
            try {
                ReceivedMessage receivedMessage = new ReceivedMessage(
                        BrokersType.WEBSOCKET,
                        destination,
                        message,
                        messageId,
                        java.time.Instant.now(),
                        java.util.Map.of("source", "local")
                );

                listener.onMessage(receivedMessage, destination);

                log.debug("Notified local subscriber for destination: {}", destination);

            } catch (Exception e) {
                log.error("Error notifying local subscriber for destination {}: {}",
                        destination, e.getMessage(), e);
            }
        }
    }

    /**
     * Обрабатывает входящее WebSocket сообщение от клиентов.
     * Этот метод может быть вызван из WebSocket контроллера.
     *
     * @param message текст сообщения
     * @param destination тема сообщения
     */
    public void handleIncomingWebSocketMessage(String message, String destination) {
        try {
            log.debug("Received WebSocket message from destination: {} - {}", destination, message);

            MessageListener listener = activeSubscriptions.get(destination);
            if (listener != null) {
                ReceivedMessage receivedMessage = new ReceivedMessage(
                        BrokersType.WEBSOCKET,
                        destination,
                        message,
                        UUID.randomUUID().toString(),
                        java.time.Instant.now(),
                        java.util.Map.of("source", "websocket")
                );

                listener.onMessage(receivedMessage, destination);

                log.debug("Processed incoming WebSocket message for destination: {}", destination);
            } else {
                log.debug("No local subscriber found for destination: {}", destination);
            }

        } catch (Exception e) {
            log.error("Error processing incoming WebSocket message for destination {}: {}",
                    destination, e.getMessage(), e);
        }
    }

    /**
     * "Отключает" WebSocket брокер.
     * Очищает все активные подписки и сбрасывает флаг connected.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting WebSocket broker...");

        try {
            // Очищаем все активные подписки
            activeSubscriptions.clear();

            connected = false;

            log.info("WebSocket broker successfully disconnected");

        } catch (Exception e) {
            log.warn("Error during WebSocket broker disconnect: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверяет наличие активного подключения WebSocket брокера.
     *
     * @return true если брокер подключен, иначе false
     */
    @Override
    public boolean isConnected() {
        return connected && messagingTemplate != null;
    }

    /**
     * Возвращает тип брокера - WebSocket.
     *
     * @return тип брокера {@link BrokersType#WEBSOCKET}
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.WEBSOCKET;
    }

    /**
     * Проверяет работоспособность брокера.
     * Проверяет что брокер подключен и messagingTemplate доступен.
     *
     * @return true если брокер работает корректно, иначе false
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }

    /**
     * Возвращает количество активных подписок.
     *
     * @return количество активных подписок
     */
    public int getActiveSubscriptionsCount() {
        return activeSubscriptions.size();
    }
}
