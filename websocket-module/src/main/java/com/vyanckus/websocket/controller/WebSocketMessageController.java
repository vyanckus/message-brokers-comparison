package com.vyanckus.websocket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket контроллер для обработки сообщений в реальном времени.
 */
@Controller
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Обрабатывает сообщения отправленные на /app/livedata
     */
    @MessageMapping("/livedata")
    @SendTo("/topic/livedata")
    public Map<String, Object> handleLiveData(Map<String, Object> message) {
        // Добавляем timestamp и обрабатываем сообщение
        return Map.of(
                "type", message.get("type"),
                "value", message.get("value"),
                "timestamp", Instant.now().toString(),
                "processed", true
        );
    }

    /**
     * Отправляет тестовые данные в реальном времени
     */
    public void sendLiveData(String type, double value) {
        Map<String, Object> data = Map.of(
                "type", type,
                "value", value,
                "timestamp", Instant.now().toString(),
                "source", "server"
        );

        messagingTemplate.convertAndSend("/topic/livedata", data);
    }

    /**
     * Отправляет статистику
     */
    public void sendStatistics(Map<String, Object> stats) {
        messagingTemplate.convertAndSend("/topic/statistics", stats);
    }
}
