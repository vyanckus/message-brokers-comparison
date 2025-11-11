package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object для отправки сообщения в брокер сообщений.
 *
 * <p>Record обеспечивает иммутабельность и автоматическую реализацию
 * методов {@code equals()}, {@code hashCode()}, {@code toString()}.
 *
 * @param brokerType тип брокера, в который отправляется сообщение (обязательно)
 * @param destination назначение сообщения (очередь, топик, endpoint) (обязательно)
 * @param message тело сообщения (обязательно)
 * @param headers дополнительные заголовки сообщения (опционально)
 *
 * @see MessageResponse
 * @see ReceivedMessage
 */
public record MessageRequest(
        /**
         * Тип брокера, в который отправляется сообщение.
         * Определяет, какой модуль будет использоваться для отправки.
         */
        @NotNull(message = "Broker type cannot be null")
        BrokersType brokerType,

        /**
         * Назначение сообщения в зависимости от типа брокера:
         * <ul>
         *   <li>Для ActiveMQ/RabbitMQ - имя очереди или топика</li>
         *   <li>Для Kafka - имя топика</li>
         *   <li>Для WebSocket - endpoint URL</li>
         * </ul>
         */
        @NotBlank(message = "Destination cannot be blank")
        String destination,

        /**
         * Тело сообщения в виде строки.
         */
        @NotBlank(message = "Message cannot be blank")
        String message,

        /**
         * Дополнительные заголовки сообщения.
         * Могут содержать метаданные, параметры маршрутизации, TTL, приоритет и т.д.
         * Если не указаны, используется пустая неизменяемая Map.
         */
        Map<String, Object> headers
) {

    /**
     * Конструктор для десериализации JSON с помощью Jackson.
     * Гарантирует, что все обязательные поля не-null и устанавливает значения по умолчанию.
     *
     * @param brokerType тип брокера (обязательно)
     * @param destination назначение сообщения (обязательно)
     * @param message тело сообщения (обязательно)
     * @param headers заголовки сообщения (опционально)
     *
     * @throws NullPointerException если любой из обязательных параметров равен {@code null}
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
     * Упрощенный конструктор для создания сообщения без заголовков.
     *
     * @param brokerType тип брокера
     * @param destination назначение сообщения
     * @param message тело сообщения
     */
    public MessageRequest(BrokersType brokerType, String destination, String message) {
        this(brokerType, destination, message, Map.of());
    }

    /**
     * Создает копию этого MessageRequest с добавленным заголовком.
     * Исходный объект остается неизменным (immutable).
     *
     * @param key ключ заголовка
     * @param value значение заголовка
     * @return новый объект MessageRequest с добавленным заголовком
     *
     * @throws NullPointerException если key равен {@code null}
     *
     * <p><b>Примечание:</b> Поскольку record иммутабелен, этот метод возвращает новый объект.
     */
    public MessageRequest withHeader(String key, Object value) {
        Map<String, Object> newHeaders = new java.util.HashMap<>(this.headers);
        newHeaders.put(key, value);
        return new MessageRequest(this.brokerType, this.destination, this.message, newHeaders);
    }

    /**
     * Извлекает значение заголовка с приведением к указанному типу.
     *
     * @param <T> тип возвращаемого значения
     * @param key ключ заголовка
     * @param type класс ожидаемого типа
     * @return значение заголовка или {@code null} если заголовок не найден
     * @throws ClassCastException если значение не может быть приведено к указанному типу
     *
     * <p><b>Примечание:</b> Использует безопасное приведение типов через {@link Class#cast(Object)}.
     */
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String key, Class<T> type) {
        Object value = headers.get(key);
        return value != null ? type.cast(value) : null;
    }
}
