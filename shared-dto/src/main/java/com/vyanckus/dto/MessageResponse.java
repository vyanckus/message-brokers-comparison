package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * DTO для ответа после отправки сообщения.
 * Содержит статус операции и метаданные.
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
         * Идентификатор сообщения (если поддерживается брокером)
         */
        String messageId,

        /**
         * Дополнительное сообщение (ошибка или информация)
         */
        String details,

        /**
         * Временная метка создания ответа
         * JsonFormat для корректной сериализации в JSON
         */
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp
) {

    /**
     * Статические фабричные методы для удобства создания
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

    public static MessageResponse error(BrokersType brokerType, String errorMessage) {
        return new MessageResponse(
                brokerType,
                "ERROR",
                null,
                errorMessage,
                Instant.now()
        );
    }

    public static MessageResponse of(BrokersType brokerType, String status, String messageId, String details) {
        return new MessageResponse(brokerType, status, messageId, details, Instant.now());
    }

    /**
     * Проверка успешности операции
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
