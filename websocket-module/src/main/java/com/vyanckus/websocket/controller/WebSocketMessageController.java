package com.vyanckus.websocket.controller;

import com.vyanckus.websocket.WebSocketBroker;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket контроллер для приема сообщений от клиентов.
 */
@Controller
public class WebSocketMessageController {

    private final WebSocketBroker webSocketBroker;

    public WebSocketMessageController(WebSocketBroker webSocketBroker) {
        this.webSocketBroker = webSocketBroker;
    }

    /**
     * Обрабатывает входящие WebSocket сообщения.
     *
     * @param message текст сообщения
     * @param destination тема сообщения
     */
    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public String handleWebSocketMessage(String message, String destination) {
        webSocketBroker.handleIncomingWebSocketMessage(message, destination);
        return message;
    }
}
