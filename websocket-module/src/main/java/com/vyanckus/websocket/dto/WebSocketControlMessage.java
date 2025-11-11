package com.vyanckus.websocket.dto;

import java.util.Map;

/**
 * DTO для управления WebSocket генератором данных с клиента.
 * Используется для отправки команд запуска/остановки и параметров генерации.
 *
 * @param command команда для выполнения (start, stop)
 * @param parameters параметры генерации данных
 */
public record WebSocketControlMessage(
        String command,
        Map<String, Object> parameters
) {
    /**
     * Упрощенный конструктор для команд без параметров.
     *
     * @param command команда для выполнения
     */
    public WebSocketControlMessage(String command) {
        this(command, Map.of());
    }
}
