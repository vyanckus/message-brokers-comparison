package com.vyanckus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object для запроса на подписку на сообщения от брокера.
 * Используется для регистрации потребителей сообщений из очередей, топиков или WebSocket endpoints.
 *
 * @param brokerType тип брокера для подписки
 * @param destination назначение (topic, queue) на которое подписываемся
 * @param subscriptionId уникальный идентификатор подписки
 *
 * @see ReceivedMessage
 */
public record SubscriptionRequest(

        /**
         * Тип брокера, на который создается подписка.
         * Определяет протокол и механизм доставки сообщений.
         */
        @NotNull(message = "Broker type cannot be null")
        BrokersType brokerType,

        /**
         * Назначение (topic, queue), на которое создается подписка.
         */
        @NotBlank(message = "Destination cannot be blank")
        String destination,

        /**
         * Уникальный идентификатор подписки.
         *
         * <p><b>Назначение:</b>
         * <ul>
         *   <li>Идентификация подписки для управления (отписка)</li>
         *   <li>Трассировка в логах и мониторинге</li>
         *   <li>Связывание с пользовательскими сессиями (WebSocket)</li>
         * </ul>
         *
         * <p>Если не указан, генерируется автоматически как UUID.
         */
        String subscriptionId
) {

    /**
     * Компактный конструктор для автоматической генерации subscriptionId.
     * Если subscriptionId не указан или пустой, генерируется случайный UUID.
     */
    public SubscriptionRequest {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * Упрощенный конструктор для создания подписки с автоматически сгенерированным ID.
     *
     * @param brokerType тип брокера
     * @param destination назначение
     */
    public SubscriptionRequest(BrokersType brokerType, String destination) {
        this(brokerType, destination, java.util.UUID.randomUUID().toString());
    }
}
