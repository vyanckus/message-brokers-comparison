package com.vyanckus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для запроса на подписку на сообщения от брокера
 */
public record SubscriptionRequest(

        /**
         * Тип брокера для подписки
         */
        @NotNull(message = "Broker type cannot be null")
        BrokersType brokerType,

        /**
         * Назначение (topic, queue) на которое подписываемся
         */
        @NotBlank(message = "Destination cannot be blank")
        String destination,

        /**
         * ID подписки (для идентификации в WebSocket и т.д.)
         */
        String subscriptionId
) {

    public SubscriptionRequest {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * Упрощенный конструктор
     */
    public SubscriptionRequest(BrokersType brokerType, String destination) {
        this(brokerType, destination, java.util.UUID.randomUUID().toString());
    }
}
