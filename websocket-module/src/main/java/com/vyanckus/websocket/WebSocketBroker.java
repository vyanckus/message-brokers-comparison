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

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реализация {@link MessageBroker} для WebSocket соединений.
 * Использует STOMP over WebSocket для реального времени двусторонней связи.
 *
 * <p><b>Важно:</b> WebSocket является каналом связи, а не брокером сообщений.
 * Для унификации API с другими брокерами (Kafka, RabbitMQ) используется эмуляция
 * поведения брокера через внутреннюю систему подписок.</p>
 *
 * <p><b>Особенности реализации:</b>
 * <ul>
 *   <li>Spring SimpMessagingTemplate для отправки сообщений</li>
 *   <li>Local subscription tracking для эмуляции получения сообщений</li>
 *   <li>Automatic connection management by Spring</li>
 *   <li>Упрощенная логика для максимальной производительности</li>
 * </ul>
 *
 * @see MessageBroker
 * @see BrokersType#WEBSOCKET
 */
@Component
public class WebSocketBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroker.class);

    /**
     * Конфигурация WebSocket из application.yml.
     */
    private final BrokerProperties.WebSocketProperties config;

    /**
     * Spring Messaging Template для отправки сообщений через WebSocket.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Активные подписки для эмуляции поведения брокера сообщений.
     * WebSocket не имеет встроенной системы подписок на стороне сервера,
     * поэтому мы эмулируем её через этот Map для унификации API.
     */
    private final ConcurrentMap<String, MessageListener> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * Флаг подключения WebSocket брокера.
     * В отличие от других брокеров, здесь флаг указывает на доступность
     * messagingTemplate, а не на установленное сетевое соединение.
     */
    private boolean connected = false;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerProperties конфигурация всех брокеров
     * @param messagingTemplate Spring Messaging Template для WebSocket
     */
    public WebSocketBroker(BrokerProperties brokerProperties, SimpMessagingTemplate messagingTemplate) {
        this.config = Optional.ofNullable(brokerProperties.websocket())
                .orElse(new BrokerProperties.WebSocketProperties("localhost", 8080, "/topic/messages", true));

        this.messagingTemplate = messagingTemplate;
        log.info("WebSocket broker initialized with endpoint: {}:{}",
                config.endpoint(), config.port());
    }

    /**
     * Инициализирует WebSocket брокер.
     * В отличие от других брокеров, здесь не устанавливается сетевое соединение,
     * а проверяется доступность Spring WebSocket инфраструктуры.
     * WebSocket подключение управляется Spring автоматически при подключении клиентов.
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
            if (messagingTemplate == null) {
                throw new BrokerConnectionException("WebSocket messaging template is not available");
            }

            connected = true;
            log.info("WebSocket broker successfully initialized");

        } catch (Exception e) {
            String errorMessage = "Failed to initialize WebSocket broker";
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение через WebSocket на указанный destination.
     *
     * <p><b>Эмуляция брокера:</b> Параллельно с отправкой реальным WebSocket клиентам,
     * уведомляются внутренние подписчики через {@link #notifyLocalSubscribers},
     * создавая иллюзию работы с полноценным брокером сообщений.</p>
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
            String destination = request.destination() != null ? request.destination() : config.path();

            // Упрощенная отправка через WebSocket
            messagingTemplate.convertAndSend(destination, request.message());

            // Простой ID для демо
            String messageId = "websocket-" + System.nanoTime();

            // Уведомляем локальных подписчиков
            notifyLocalSubscribers(destination, request.message(), messageId);

            return MessageResponse.success(BrokersType.WEBSOCKET, messageId);

        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage());
            throw new MessageSendException("Failed to send WebSocket message", e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанного destination.
     *
     * <p><b>Эмуляция брокера:</b> WebSocket не поддерживает подписки на стороне сервера.
     * Этот метод регистрирует слушатель для эмуляции получения сообщений
     * через {@link #notifyLocalSubscribers} и {@link #handleIncomingWebSocketMessage}.</p>
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
            activeSubscriptions.put(destination, listener);
            log.info("Subscribed to WebSocket destination: {}", destination);

        } catch (Exception e) {
            String errorMessage = "Failed to subscribe to WebSocket destination " + destination;
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Уведомляет локальных подписчиков о новом сообщении.
     *
     * <p><b>Эмуляция брокера:</b> Этот метод является ключевым для эмуляции
     * поведения брокера сообщений. Он создаёт иллюзию, что сообщение было
     * получено от внешнего брокера, а не отправлено через тот же WebSocketBroker.</p>
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

            } catch (Exception e) {
                log.error("Error notifying local subscriber for destination {}: {}", destination, e.getMessage());
            }
        }
    }

    /**
     * Обрабатывает входящее WebSocket сообщение от клиентов.
     *
     * <p><b>Эмуляция брокера:</b> Этот метод используется для обработки сообщений,
     * которые реально пришли от WebSocket клиентов, и передачи их внутренним
     * подписчикам как будто они пришли от внешнего брокера.</p>
     *
     * <p><b>Требует интеграции:</b> Для работы этого метода необходимо, чтобы
     * WebSocket контроллеры вызывали его при получении сообщений от клиентов.</p>
     *
     * @param message текст сообщения от клиента
     * @param destination тема сообщения
     */
    public void handleIncomingWebSocketMessage(String message, String destination) {
        try {
            MessageListener listener = activeSubscriptions.get(destination);
            if (listener != null) {
                ReceivedMessage receivedMessage = new ReceivedMessage(
                        BrokersType.WEBSOCKET,
                        destination,
                        message,
                        "websocket-recv-" + System.nanoTime(),
                        java.time.Instant.now(),
                        java.util.Map.of("source", "websocket")
                );

                listener.onMessage(receivedMessage, destination);
            }

        } catch (Exception e) {
            log.error("Error processing incoming WebSocket message for destination {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Отписывается от получения сообщений из указанного destination.
     *
     * @param destination тема от которой отписываемся
     * @throws SubscriptionException если не удалось отписаться
     */
    @Override
    public void unsubscribe(String destination) throws SubscriptionException {
        MessageListener listener = activeSubscriptions.remove(destination);
        if (listener == null) {
            log.warn("No active subscription found for: {}", destination);
            return;
        }

        log.info("Unsubscribed from: {}", destination);
    }

    /**
     * Отписывается от всех активных подписок WebSocket.
     */
    @Override
    public void unsubscribeAll() throws SubscriptionException {
        log.info("Unsubscribing from all WebSocket destinations...");

        activeSubscriptions.keySet().forEach(destination -> {
            try {
                unsubscribe(destination);
            } catch (SubscriptionException e) {
                log.warn("Failed to unsubscribe from {}: {}", destination, e.getMessage());
            }
        });

        log.info("Unsubscribed from all WebSocket destinations");
    }

    /**
     * Отключает WebSocket брокер и освобождает ресурсы.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting WebSocket broker...");

        try {
            unsubscribeAll();
        } catch (Exception e) {
            log.warn("Error during unsubscribe: {}", e.getMessage());
        }

        connected = false;
        log.info("WebSocket broker disconnected");
    }

    /**
     * Проверяет наличие активного подключения WebSocket брокера.
     */
    @Override
    public boolean isConnected() {
        return connected && messagingTemplate != null;
    }

    /**
     * Возвращает тип брокера - WebSocket.
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.WEBSOCKET;
    }

    /**
     * Проверяет работоспособность брокера.
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }

    /**
     * Возвращает количество активных подписок.
     */
    public int getActiveSubscriptionsCount() {
        return activeSubscriptions.size();
    }
}
