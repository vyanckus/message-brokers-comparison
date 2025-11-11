package com.vyanckus.dto;

/**
 * Перечисление всех поддерживаемых типов брокеров сообщений в системе.
 * Используется для идентификации типа брокера в запросах, ответах и конфигурации.
 */
public enum BrokersType {
    /**
     * Apache ActiveMQ - JMS-совместимый брокер сообщений.
     * Поддерживает очереди и топики, работает по протоколу JMS.
     */
    ACTIVEMQ("ActiveMQ"),

    /**
     * RabbitMQ - брокер сообщений, реализующий протокол AMQP 0-9-1.
     * Поддерживает сложные маршрутизации через exchange и binding.
     */
    RABBITMQ("RabbitMQ"),

    /**
     * Apache Kafka - распределенная streaming-платформа.
     * Оптимизирована для высокопроизводительной обработки потоков данных.
     */
    KAFKA("Kafka"),

    /**
     * WebSocket - протокол для полноценной двусторонней связи поверх HTTP.
     * Хотя не является классическим брокером, включен для сравнения производительности.и
     */
    WEBSOCKET("WebSocket");

    private final String displayName;

    /**
     * Создает новый тип брокера с указанным отображаемым именем.
     *
     * @param displayName читаемое имя для отображения в UI
     */

    BrokersType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Возвращает читаемое имя брокера для отображения в пользовательском интерфейсе.
     *
     * @return отображаемое имя брокера (например, "ActiveMQ" вместо "ACTIVEMQ")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Преобразует строковое значение в соответствующий тип брокера.
     * Поиск выполняется без учета регистра как по имени enum, так и по отображаемому имени.
     *
     * @param value строковое значение для преобразования
     * @return соответствующий тип брокера или {@code null} если передан {@code null}
     * @throws IllegalArgumentException если значение не соответствует ни одному известному типу брокера
     *
     * <p><b>Примечание:</b> Поддерживаемые форматы: "kafka", "KAFKA", "Kafka", "activemq", "ActiveMQ" и т.д.
     *
     * <p><b>Примеры:</b>
     * <pre>{@code
     * BrokersType.fromString("kafka")     // → KAFKA
     * BrokersType.fromString("ActiveMQ")  // → ACTIVEMQ
     * BrokersType.fromString("RABBITMQ")  // → RABBITMQ
     * }</pre>
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
