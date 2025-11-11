package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Data Transfer Object для сообщений, полученных от брокеров сообщений.
 * Представляет входящее сообщение с метаданными о источнике и времени получения.
 *
 * @param brokerType тип брокера, от которого пришло сообщение
 * @param destination назначение (topic, queue) откуда пришло сообщение
 * @param message тело сообщения
 * @param messageId ID сообщения (если поддерживается брокером)
 * @param receivedAt временная метка получения сообщения
 * @param properties дополнительные свойства/заголовки сообщения
 *
 * @see MessageRequest
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
         * Временная метка получения сообщения приложением.
         * Форматируется в ISO 8601 в UTC временной зоне.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant receivedAt,

        /**
         * Дополнительные свойства и заголовки сообщения.
         *
         * <p><b>Примеры содержимого:</b>
         * <ul>
         *   <li>RabbitMQ: delivery mode, priority, headers</li>
         *   <li>Kafka: partition, offset, headers</li>
         *   <li>ActiveMQ: JMS properties, priority, timestamp</li>
         * </ul>
         *
         * <p>Если не указаны, используется пустая неизменяемая Map.
         */
        java.util.Map<String, Object> properties
) {

    /**
     * Компактный конструктор для установки значений по умолчанию.
     * Выполняется перед инициализацией полей record.
     *
     * <p><b>Логика инициализации:</b>
     * <ul>
     *   <li>Если properties = {@code null}, устанавливается пустая Map</li>
     *   <li>Если receivedAt = {@code null}, устанавливается текущее время</li>
     * </ul>
     */
    public ReceivedMessage {
        if (properties == null) {
            properties = java.util.Map.of();
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    /**
     * Создает ReceivedMessage с основными параметрами.
     * Автоматически устанавливает receivedAt в текущее время и properties в пустую Map.
     *
     * @param brokerType тип брокера
     * @param destination назначение
     * @param message тело сообщения
     * @return новый объект ReceivedMessage
     *
     * <p><b>Примечание:</b> messageId устанавливается в {@code null}
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
     * Создает ReceivedMessage с указанным messageId.
     * Автоматически устанавливает receivedAt в текущее время и properties в пустую Map.
     *
     * @param brokerType тип брокера
     * @param destination назначение
     * @param message тело сообщения
     * @param messageId идентификатор сообщения
     * @return новый объект ReceivedMessage
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
