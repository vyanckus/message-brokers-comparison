package com.vyanckus.dto;

/**
 * Перечисление всех поддерживаемых типов брокеров сообщений.
 * Используется для идентификации типа брокера в запросах и ответах.
 */
public enum BrokersType {
    /**
     * Apache ActiveMQ - JMS брокер
     */
    ACTIVEMQ("ActiveMQ"),

    /**
     * RabbitMQ - AMQP брокер
     */
    RABBITMQ("RabbitMQ"),

    /**
     * Apache Kafka - распределенный streaming-платформа
     */
    KAFKA("Kafka"),

    /**
     * WebSocket - протокол для двусторонней связи
     */
    WEBSOCKET("WebSocket");

    private final String displayName;

    BrokersType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Возвращает читаемое имя брокера
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Поиск по строковому значению (case-insensitive)
     */
    public static BrokersType fromString(String value) {
        if (value == null) return null;

        for (BrokersType type : values()) {
            if (type.name().equalsIgnoreCase(value) ||
                    type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown broker type: " + value);
    }
}
