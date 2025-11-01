package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * DTO для отправки сообщения в брокер.
 * Record обеспечивает иммутабельность и автоматические методы equals/hashCode/toString.
 */
public record MessageRequest(
        /**
         * Тип брокера, в который отправляется сообщение
         * Обязательное поле - валидация на уровне API
         */
        @NotNull(message = "Broker type cannot be null")
        BrokersType brokerType,

        /**
         * Назначение сообщения (queue, topic, endpoint)
         * Для ActiveMQ/RabbitMQ - имя очереди
         * Для Kafka - имя топика
         * Для WebSocket - endpoint
         */
        @NotBlank(message = "Destination cannot be blank")
        String destination,

        /**
         * Тело сообщения
         */
        @NotBlank(message = "Message cannot be blank")
        String message,

        /**
         * Дополнительные заголовки сообщения
         * Могут содержать метаданные, параметры маршрутизации и т.д.
         */
        Map<String, Object> headers
) {

    /**
     * Кастомный конструктор с дефолтными значениями
     * JsonCreator помогает Jackson создавать объекты из JSON
     */
    @JsonCreator
    public MessageRequest(
            @JsonProperty("brokerType") BrokersType brokerType,
            @JsonProperty("destination") String destination,
            @JsonProperty("message") String message,
            @JsonProperty("headers") Map<String, Object> headers) {

        this.brokerType = Objects.requireNonNull(brokerType, "brokerType cannot be null");
        this.destination = Objects.requireNonNull(destination, "destination cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Map.of();
    }

    /**
     * Упрощенный конструктор без headers
     */
    public MessageRequest(BrokersType brokerType, String destination, String message) {
        this(brokerType, destination, message, Map.of());
    }

    /**
     * Вспомогательный метод для добавления header
     * Возвращает новый объект (record иммутабелен)
     */
    public MessageRequest withHeader(String key, Object value) {
        Map<String, Object> newHeaders = new java.util.HashMap<>(this.headers);
        newHeaders.put(key, value);
        return new MessageRequest(this.brokerType, this.destination, this.message, newHeaders);
    }

    /**
     * Получение header с приведением типа
     */
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String key, Class<T> type) {
        Object value = headers.get(key);
        return value != null ? type.cast(value) : null;
    }
}
