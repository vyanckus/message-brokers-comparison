package com.vyanckus.controller;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.metrics.BrokerMetrics;
import com.vyanckus.service.MessageBrokerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST контроллер для работы с сообщениями через брокеры.
 *
 * <p>Предоставляет API endpoints для:</p>
 * <ul>
 *   <li>Инициализации брокеров</li>
 *   <li>Отправки сообщений через разные брокеры</li>
 *   <li>Подписки на получение сообщений</li>
 *   <li>Мониторинга статуса брокеров</li>
 *   <li>Получения истории сообщений</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    private static final int MAX_HISTORY_SIZE = 100;

    private final MessageBrokerService messageBrokerService;
    private final BrokerMetrics brokerMetrics;

    /**
     * История полученных сообщений для API.
     */
    private final List<ReceivedMessage> messageHistory = new CopyOnWriteArrayList<>();

    /**
     * Конструктор контроллера работы с сообщениями.
     *
     * @param messageBrokerService сервис для работы с брокерами сообщений
     * @param brokerMetrics компонент для сбора метрик брокеров
     */
    public MessageController(MessageBrokerService messageBrokerService, BrokerMetrics brokerMetrics) {
        this.messageBrokerService = messageBrokerService;
        this.brokerMetrics = brokerMetrics;
        // Регистрируем себя как слушателя сообщений
        this.messageBrokerService.addMessageListener(this::handleReceivedMessage);
        log.info("MessageController initialized");
    }

    /**
     * Инициализирует все брокеры сообщений.
     * Должен быть вызван перед использованием других endpoints.
     *
     * @return результат инициализации с временной меткой
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeBrokers() {
        try {
            validateServiceNotInitialized();

            messageBrokerService.initialize();
            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "All message brokers initialized successfully",
                    "timestamp", java.time.Instant.now()
            );
            log.info("Brokers initialization requested via API");
            return ResponseEntity.ok(response);
        } catch (BrokerConnectionException e) {
            log.error("Brokers initialization failed: {}", e.getMessage());
            return createErrorResponse("Failed to initialize brokers: " + e.getMessage());
        } catch (IllegalStateException e) {
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Отправляет сообщение через указанный брокер.
     * Поддерживает все типы брокеров через единый интерфейс.
     *
     * @param request запрос на отправку сообщения
     * @return результат отправки с ID сообщения и метаданными
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@Valid @RequestBody MessageRequest request) {
        try {
            validateServiceInitialized();

            MessageResponse response = messageBrokerService.sendMessage(request);
            Map<String, Object> successResponse = Map.of(
                    "status", "SUCCESS",
                    "brokerType", response.brokerType(),
                    "messageId", response.messageId(),
                    "details", response.details(),
                    "timestamp", response.timestamp()
            );
            log.debug("Message sent via {} broker to: {}", request.brokerType(), request.destination());
            return ResponseEntity.ok(successResponse);
        } catch (MessageSendException e) {
            log.error("Message sending failed: {}", e.getMessage());
            return createErrorResponse("Failed to send message: " + e.getMessage());
        } catch (IllegalStateException e) {
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Проверяет инициализирован ли сервис брокеров.
     * Выбрасывает исключение если брокеры не инициализированы.
     *
     * @throws IllegalStateException если сервис брокеров не инициализирован
     */
    private void validateServiceInitialized() {
        if (!messageBrokerService.isInitialized()) {
            throw new IllegalStateException("Message brokers not initialized. Call /api/messages/initialize first.");
        }
    }

    /**
     * Проверяет что сервис брокеров еще не инициализирован.
     * Выбрасывает исключение если брокеры уже инициализированы.
     *
     * @throws IllegalStateException если сервис брокеров уже инициализирован
     */
    private void validateServiceNotInitialized() {
        if (messageBrokerService.isInitialized()) {
            throw new IllegalStateException("Message brokers already initialized");
        }
    }

    /**
     * Подписывается на получение сообщений из указанного брокера.
     *
     * @param brokerType тип брокера
     * @param destination назначение (очередь, топик)
     * @return ResponseEntity с результатом подписки
     */
    @PostMapping("/subscribe/{brokerType}")
    public ResponseEntity<Map<String, Object>> subscribe(@PathVariable BrokersType brokerType, @RequestParam String destination) {
        try {
            validateServiceInitialized();

            messageBrokerService.subscribe(brokerType, destination);
            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Subscribed to " + destination + " via " + brokerType + " broker",
                    "brokerType", brokerType,
                    "destination", destination,
                    "timestamp", java.time.Instant.now()
            );
            log.info("Subscription created via API: {} -> {}", brokerType, destination);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Failed to subscribe: " + e.getMessage());
        }
    }

    /**
     * Запуск конкретного брокера (реальная реализация)
     */
    @PostMapping("/{brokerType}/start")
    public ResponseEntity<Map<String, Object>> startBroker(@PathVariable String brokerType) {
        try {
            // Конвертируем строку в enum (case-insensitive)
            BrokersType type = BrokersType.fromString(brokerType);
            if (type == null) {
                return createErrorResponse("Unknown broker type: " + brokerType);
            }

            validateServiceInitialized();

            // Получаем брокер и подключаем его
            MessageBroker broker = messageBrokerService.getBroker(type);
            if (broker != null) {
                if (!broker.isConnected()) {
                    broker.connect();
                }

                Map<String, Object> response = Map.of(
                        "status", "SUCCESS",
                        "message", "Broker " + type + " started successfully",
                        "brokerType", type,
                        "connected", broker.isConnected(),
                        "timestamp", java.time.Instant.now()
                );
                log.info("Broker {} started via API", type);
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Broker " + type + " not available or not configured");
            }
        } catch (Exception e) {
            log.error("Failed to start broker {}: {}", brokerType, e.getMessage());
            return createErrorResponse("Failed to start broker " + brokerType + ": " + e.getMessage());
        }
    }

    /**
     * Остановка конкретного брокера (реальная реализация)
     */
    @PostMapping("/{brokerType}/stop")
    public ResponseEntity<Map<String, Object>> stopBroker(@PathVariable String brokerType) {
        try {
            // Конвертируем строку в enum (case-insensitive)
            BrokersType type = BrokersType.fromString(brokerType);
            if (type == null) {
                return createErrorResponse("Unknown broker type: " + brokerType);
            }

            validateServiceInitialized();

            MessageBroker broker = messageBrokerService.getBroker(type);
            if (broker != null && broker.isConnected()) {
                broker.disconnect();

                Map<String, Object> response = Map.of(
                        "status", "SUCCESS",
                        "message", "Broker " + type + " stopped successfully",
                        "brokerType", type,
                        "connected", false,
                        "timestamp", java.time.Instant.now()
                );
                log.info("Broker {} stopped via API", type);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "status", "SUCCESS",
                        "message", "Broker " + type + " already stopped",
                        "brokerType", type,
                        "connected", false,
                        "timestamp", java.time.Instant.now()
                );
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to stop broker {}: {}", brokerType, e.getMessage());
            return createErrorResponse("Failed to stop broker " + brokerType + ": " + e.getMessage());
        }
    }

    /**
     * Отправка сообщения через конкретный брокер (упрощенная версия для frontend)
     */
    @PostMapping("/{brokerType}/send")
    public ResponseEntity<Map<String, Object>> sendMessageToBroker(@PathVariable String brokerType,
                                                                   @RequestBody Map<String, String> request) {

        try {
            // Конвертируем строку в enum
            BrokersType type = BrokersType.fromString(brokerType);
            if (type == null) {
                return createErrorResponse("Unknown broker type: " + brokerType);
            }

            validateServiceInitialized();

            String message = request.get("message");
            String destination = request.get("destination") != null ? request.get("destination") : "test.queue";

            if (message == null || message.isBlank()) {
                return createErrorResponse("Message cannot be empty");
            }

            MessageRequest messageRequest = new MessageRequest(type, destination, message); // ИСПОЛЬЗУЕМ type
            MessageResponse response = messageBrokerService.sendMessage(messageRequest);

            Map<String, Object> successResponse = Map.of(
                    "status", "SUCCESS",
                    "brokerType", response.brokerType(),
                    "messageId", response.messageId(),
                    "details", response.details(),
                    "timestamp", response.timestamp()
            );
            return ResponseEntity.ok(successResponse);
        } catch (MessageSendException e) {
            return createErrorResponse("Failed to send message: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Возвращает статус всех брокеров.
     *
     * @return ResponseEntity со статусом брокеров
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBrokersStatus() {
        Map<BrokersType, Boolean> status = messageBrokerService.getBrokersStatus();
        Map<BrokersType, Boolean> health = messageBrokerService.getBrokersHealth();

        Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "brokers", status,
                "health", health,
                "initialized", messageBrokerService.isInitialized(),
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает историю полученных сообщений.
     *
     * @return ResponseEntity с историей сообщений
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getMessageHistory() {
        Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "messageCount", messageHistory.size(),
                "messages", messageHistory,
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает детальные метрики всех брокеров для веб-интерфейса.
     * Включает статус, здоровье, счетчики сообщений и другую статистику.
     *
     * @return ResponseEntity с метриками брокеров
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getBrokersMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // Получаем базовый статус
            Map<BrokersType, Boolean> status = messageBrokerService.getBrokersStatus();
            Map<BrokersType, Boolean> health = messageBrokerService.getBrokersHealth();

            boolean isInitialized = messageBrokerService.isInitialized();

            // Собираем детальные метрики для каждого брокера
            for (BrokersType brokerType : BrokersType.values()) {
                Map<String, Object> brokerMetrics = new HashMap<>();

                if (!isInitialized) {
                    brokerMetrics.put("status", "NOT_INITIALIZED");
                    brokerMetrics.put("health", false);
                    brokerMetrics.put("messagesSent", 0);
                    brokerMetrics.put("messagesReceived", 0);
                    brokerMetrics.put("averageLatency", 0);
                } else {
                    // Базовый статус
                    brokerMetrics.put("status", status.getOrDefault(brokerType, false) ? "RUNNING" : "STOPPED");
                    brokerMetrics.put("health", health.getOrDefault(brokerType, false));

                    // Метрики производительности (заглушки - нужно реализовать в сервисе)
                    brokerMetrics.put("messagesSent", getMessageCount(brokerType));
                    brokerMetrics.put("messagesReceived", getReceivedMessageCount(brokerType));
                    brokerMetrics.put("averageLatency", getAverageLatency(brokerType));
                }

                brokerMetrics.put("lastActivity", java.time.Instant.now().toString());
                metrics.put(brokerType.name().toLowerCase(), brokerMetrics);
            }

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "metrics", metrics,
                    "timestamp", java.time.Instant.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get brokers metrics: {}", e.getMessage());
            return createErrorResponse("Failed to get metrics: " + e.getMessage());
        }
    }

    /**
     * Очищает историю сообщений.
     *
     * @return ResponseEntity с результатом очистки
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearMessageHistory() {
        int previousSize = messageHistory.size();
        messageHistory.clear();

        Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Message history cleared",
                "clearedMessages", previousSize,
                "timestamp", java.time.Instant.now()
        );

        log.info("Message history cleared via API. Removed {} messages", previousSize);
        return ResponseEntity.ok(response);
    }

    /**
     * Обрабатывает полученное сообщение от любого брокера.
     * Сохраняет сообщение в историю и обеспечивает thread-safe доступ.
     *
     * @param message полученное сообщение
     * @param destination назначение сообщения
     */
    private void handleReceivedMessage(ReceivedMessage message, String destination) {
        try {
            // Сохраняем сообщение в историю
            messageHistory.add(message);

            // Ограничиваем размер истории (последние 100 сообщений)
            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(0);
            }

            log.debug("Message received and saved to history: {} -> {}", message.brokerType(), message.message());

        } catch (Exception e) {
            log.error("Error handling received message: {}", e.getMessage(), e);
        }
    }

    /**
     * Создает стандартный ответ с ошибкой.
     *
     * @param errorMessage сообщение об ошибке
     * @return ResponseEntity с ошибкой
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = Map.of(
                "status", "ERROR",
                "message", errorMessage,
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Возвращает реальное количество отправленных сообщений для брокера.
     * Использует BrokerMetrics для получения актуальных данных.
     */
    private long getMessageCount(BrokersType brokerType) {
        return brokerMetrics.getSentMessageCount(brokerType);
    }

    /**
     * Возвращает реальное количество полученных сообщений для брокера.
     * Использует BrokerMetrics для получения актуальных данных.
     */
    private long getReceivedMessageCount(BrokersType brokerType) {
        return brokerMetrics.getReceivedMessageCount(brokerType);
    }

    /**
     * Возвращает реальную среднюю задержку для брокера.
     * Использует BrokerMetrics для получения актуальных данных.
     */
    private double getAverageLatency(BrokersType brokerType) {
        return brokerMetrics.getAverageLatency(brokerType);
    }
}
