package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Data Transfer Object для ответа после отправки сообщения в брокер.
 * Содержит статус операции, идентификатор сообщения и метаданные.
 *
 * <p><b>Жизненный цикл сообщения:</b>
 * <ol>
 *   <li>Клиент создает {@link MessageRequest}</li>
 *   <li>Система отправляет сообщение в брокер</li>
 *   <li>Возвращает {@code MessageResponse} с результатом операции</li>
 * </ol>
 *
 * @param brokerType тип брокера, в который было отправлено сообщение
 * @param status статус операции отправки
 * @param messageId идентификатор сообщения (если поддерживается брокером)
 * @param details дополнительное сообщение (ошибка или информация)
 * @param timestamp временная метка создания ответа
 *
 * @see MessageRequest
 */
public record MessageResponse(

        /**
         * Тип брокера, в который было отправлено сообщение
         */
        @NotNull
        BrokersType brokerType,

        /**
         * Статус операции: SUCCESS, ERROR, etc.
         */
        @NotBlank
        String status,

        /**
         * Идентификатор сообщения, присвоенный брокером.
         *
         * <p><b>Особенности по брокерам:</b>
         * <ul>
         *   <li>Kafka: offset + partition</li>
         *   <li>RabbitMQ: delivery tag</li>
         *   <li>ActiveMQ: JMS message ID</li>
         *   <li>WebSocket: обычно {@code null}</li>
         * </ul>
         *
         * <p>Может быть {@code null} если брокер не поддерживает идентификаторы
         * или при ошибке отправки.
         */
        String messageId,

        /**
         * Дополнительное сообщение с описанием результата операции.
         *
         * <p><b>Примеры:</b>
         * <ul>
         *   <li>При успехе: "Message sent successfully to queue 'orders'"</li>
         *   <li>При ошибке: "Connection timeout to Kafka broker"</li>
         *   <li>При валидации: "Destination cannot be empty"</li>
         * </ul>
         */
        String details,

        /**
         * Временная метка создания ответа в формате ISO 8601.
         * Всегда указывается в UTC временной зоне.
         */
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp
) {

    /**
     * Создает успешный ответ после отправки сообщения.
     *
     * @param brokerType тип брокера
     * @param messageId идентификатор сообщения
     * @return ответ со статусом "SUCCESS" и текущей временной меткой
     *
     * <p><b>Примечание:</b> Автоматически устанавливает details в "Message sent successfully"
     */
    public static MessageResponse success(BrokersType brokerType, String messageId) {
        return new MessageResponse(
                brokerType,
                "SUCCESS",
                messageId,
                "Message sent successfully",
                Instant.now()
        );
    }

    /**
     * Создает ответ об ошибке при отправке сообщения.
     *
     * @param brokerType тип брокера
     * @param errorMessage описание ошибки
     * @return ответ со статусом "ERROR" и текущей временной меткой
     *
     * <p><b>Примечание:</b> Автоматически устанавливает messageId в {@code null}
     */
    public static MessageResponse error(BrokersType brokerType, String errorMessage) {
        return new MessageResponse(
                brokerType,
                "ERROR",
                null,
                errorMessage,
                Instant.now()
        );
    }

    /**
     * Создает кастомный ответ с указанными параметрами.
     *
     * @param brokerType тип брокера
     * @param status кастомный статус операции
     * @param messageId идентификатор сообщения
     * @param details детальное описание
     * @return ответ с указанными параметрами и текущей временной меткой
     *
     * <p><b>Примечание:</b> Используется для нестандартных статусов (например, "RETRY", "PARTIAL")
     */
    public static MessageResponse of(BrokersType brokerType, String status, String messageId, String details) {
        return new MessageResponse(brokerType, status, messageId, details, Instant.now());
    }

    /**
     * Проверяет, была ли операция отправки успешной.
     *
     * @return {@code true} если статус равен "SUCCESS", иначе {@code false}
     *
     * <p><b>Примечание:</b> Безопасно обрабатывает {@code null} статус
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
