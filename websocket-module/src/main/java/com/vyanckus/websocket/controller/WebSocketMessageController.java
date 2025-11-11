package com.vyanckus.websocket.controller;

import com.vyanckus.websocket.WebSocketBroker;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket контроллер для обработки сообщений в реальном времени.
 * Обрабатывает входящие WebSocket сообщения и управляет рассылкой через STOMP протокол.
 *
 * <p><b>Обрабатываемые endpoints:</b>
 * <ul>
 *   <li><b>/app/chat</b> - простые текстовые сообщения чата</li>
 *   <li><b>/app/livedata</b> - структурированные данные в реальном времени</li>
 *   <li><b>/app/broker-data</b> - данные для интеграции с WebSocketBroker</li>
 * </ul>
 *
 * <p><b>Рассылаемые topics:</b>
 * <ul>
 *   <li><b>/topic/chat</b> - рассылка чат-сообщений</li>
 *   <li><b>/topic/livedata</b> - структурированные данные</li>
 *   <li><b>/topic/performance</b> - метрики производительности</li>
 *   <li><b>/topic/statistics</b> - статистика брокеров</li>
 * </ul>
 *
 * @see WebSocketBroker
 */
@Controller
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketBroker webSocketBroker;

    public WebSocketMessageController(SimpMessagingTemplate messagingTemplate, WebSocketBroker webSocketBroker) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketBroker = webSocketBroker;
    }

    /**
     * Обрабатывает простые текстовые сообщения чата, отправленные на /app/chat.
     * Сообщения рассылаются всем подписчикам /topic/chat и передаются в WebSocketBroker.
     *
     * @param message текстовое сообщение от клиента
     * @return ответ с сообщением и метаданными для рассылки клиентам
     */
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public Map<String, Object> handleChatMessage(String message) {
        webSocketBroker.handleIncomingWebSocketMessage(message, "/topic/chat");
        return Map.of(
                "message", message,
                "timestamp", Instant.now().toString(),
                "type", "chat"
        );
    }

    /**
     * Обрабатывает структурированные данные в реальном времени, отправленные на /app/livedata.
     * Используется для передачи метрик, графиков и других структурированных данных.
     *
     * @param data структурированные данные от клиента
     * @return обработанные данные с добавленными метаданными
     */
    @MessageMapping("/livedata")
    @SendTo("/topic/livedata")
    public Map<String, Object> handleLiveData(Map<String, Object> data) {
        return Map.of(
                "type", data.get("type"),
                "value", data.get("value"),
                "timestamp", Instant.now().toString(),
                "processed", true
        );
    }

    /**
     * Обрабатывает данные для интеграции с системой брокеров сообщений.
     * Сообщения передаются в WebSocketBroker для уведомления внутренних подписчиков.
     *
     * @param data данные с указанием destination и сообщения
     */
    @MessageMapping("/broker-data")
    public void handleBrokerData(Map<String, Object> data) {
        String destination = (String) data.get("destination");
        String message = (String) data.get("message");

        if (destination != null && message != null) {
            webSocketBroker.handleIncomingWebSocketMessage(message, destination);
            messagingTemplate.convertAndSend(destination, Map.of(
                    "message", message,
                    "timestamp", Instant.now().toString(),
                    "source", "broker"
            ));
        }
    }

    /**
     * Отправляет данные производительности брокеров в реальном времени.
     * Используется для визуализации метрик производительности в UI.
     *
     * @param brokerType тип брокера (KAFKA, RABBITMQ, etc.)
     * @param throughput производительность в сообщениях в секунду
     */
    public void sendPerformanceData(String brokerType, double throughput) {
        Map<String, Object> data = Map.of(
                "broker", brokerType,
                "throughput", throughput,
                "timestamp", Instant.now().toString(),
                "type", "performance"
        );

        messagingTemplate.convertAndSend("/topic/performance", data);
    }

    /**
     * Отправляет статистику работы брокеров для мониторинга и визуализации.
     *
     * @param stats статистика брокеров (количество сообщений, подписчики, ошибки)
     */
    public void sendBrokerStats(Map<String, Object> stats) {
        messagingTemplate.convertAndSend("/topic/statistics", stats);
    }
}
