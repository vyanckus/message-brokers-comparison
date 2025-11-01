package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * DTO для сообщений, полученных от брокеров
 */
public record ReceivedMessage(

        /**
         * Тип брокера, от которого пришло сообщение
         */
        BrokersType brokerType,

        /**
         * Назначение (topic, queue) откуда пришло сообщение
         */
        String destination,

        /**
         * Тело сообщения
         */
        String message,

        /**
         * ID сообщения (если поддерживается брокером)
         */
        String messageId,

        /**
         * Временная метка получения сообщения
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant receivedAt,

        /**
         * Дополнительные заголовки/свойства
         */
        java.util.Map<String, Object> properties
) {

    public ReceivedMessage {
        if (properties == null) {
            properties = java.util.Map.of();
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    /**
     * Создание из входящего сообщения
     */
    public static ReceivedMessage from(BrokersType brokerType, String destination, String message) {
        return new ReceivedMessage(
                brokerType,
                destination,
                message,
                null,
                Instant.now(),
                java.util.Map.of()
        );
    }

    /**
     * Создание с ID сообщения
     */
    public static ReceivedMessage from(BrokersType brokerType, String destination, String message, String messageId) {
        return new ReceivedMessage(
                brokerType,
                destination,
                message,
                messageId,
                Instant.now(),
                java.util.Map.of()
        );
    }
}
