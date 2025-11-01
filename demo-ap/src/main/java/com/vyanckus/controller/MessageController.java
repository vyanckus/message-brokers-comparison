package com.vyanckus.controller;

import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.service.MessageBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
 *
 * @author vyanckus
 * @version 1.0
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageBrokerService messageBrokerService;

    /**
     * История полученных сообщений для API.
     */
    private final List<ReceivedMessage> messageHistory = new CopyOnWriteArrayList<>();

    public MessageController(MessageBrokerService messageBrokerService) {
        this.messageBrokerService = messageBrokerService;

        // Регистрируем слушатель для сохранения истории сообщений
        this.messageBrokerService.addMessageListener(this::handleReceivedMessage);

        log.info("MessageController initialized");
    }

    /**
     * Инициализирует все брокеры сообщений.
     *
     * @return ResponseEntity с результатом инициализации
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeBrokers() {
        try {
            messageBrokerService.initialize();

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "All message brokers initialized successfully",
                    "timestamp", java.time.Instant.now()
            );

            log.info("Brokers initialization requested via API");
            return ResponseEntity.ok(response);

        } catch (BrokerConnectionException e) {
            Map<String, Object> errorResponse = Map.of(
                    "status", "ERROR",
                    "message", "Failed to initialize brokers: " + e.getMessage(),
                    "timestamp", java.time.Instant.now()
            );

            log.error("Brokers initialization failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Отправляет сообщение через указанный брокер.
     *
     * @param request запрос на отправку сообщения
     * @return ResponseEntity с результатом отправки
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody MessageRequest request) {
        if (!messageBrokerService.isInitialized()) {
            return createErrorResponse("Message brokers not initialized. Call /api/messages/initialize first.");
        }

        try {
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
            log.error("Failed to send message via {} broker: {}", request.brokerType(), e.getMessage());
            return createErrorResponse("Failed to send message: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending message: {}", e.getMessage(), e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
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
    public ResponseEntity<Map<String, Object>> subscribe(
            @PathVariable BrokersType brokerType,
            @RequestParam String destination) {

        if (!messageBrokerService.isInitialized()) {
            return createErrorResponse("Message brokers not initialized. Call /api/messages/initialize first.");
        }

        try {
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
            log.error("Failed to subscribe to {} via {} broker: {}", destination, brokerType, e.getMessage());
            return createErrorResponse("Failed to subscribe: " + e.getMessage());
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
     * Обрабатывает полученное сообщение и сохраняет в историю.
     *
     * @param message полученное сообщение
     * @param destination назначение сообщения
     */
    private void handleReceivedMessage(ReceivedMessage message, String destination) {
        try {
            // Сохраняем сообщение в историю
            messageHistory.add(message);

            // Ограничиваем размер истории (последние 100 сообщений)
            if (messageHistory.size() > 100) {
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
}