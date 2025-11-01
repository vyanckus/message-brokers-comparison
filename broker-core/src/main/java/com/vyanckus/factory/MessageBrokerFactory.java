package com.vyanckus.factory;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.config.BrokerProperties;
import com.vyanckus.dto.BrokersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Фабрика для создания экземпляров брокеров сообщений.
 * Паттерн Factory скрывает сложность создания разных типов брокеров.
 */
@Component
public class MessageBrokerFactory {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerFactory.class);

    private final BrokerProperties brokerProperties;

    public MessageBrokerFactory(BrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    /**
     * Создает брокер указанного типа
     * @param brokerType тип брокера
     * @return реализацию MessageBroker
     * @throws IllegalArgumentException если тип брокера не поддерживается
     */
    public MessageBroker createBroker(BrokersType brokerType) {
        return switch (brokerType) {
            case ACTIVEMQ -> createActiveMqBroker();
            case RABBITMQ -> createRabbitMqBroker();
            case KAFKA -> createKafkaBroker();
            case WEBSOCKET -> createWebSocketBroker();
        };
    }

    /**
     * Создает все доступные брокеры
     * @return мапа брокеров по типу
     */
    public java.util.Map<BrokersType, MessageBroker> createAllBrokers() {
        java.util.Map<BrokersType, MessageBroker> brokers = new java.util.HashMap<>();

        for (BrokersType type : BrokersType.values()) {
            try {
                brokers.put(type, createBroker(type));
            } catch (Exception e) {
                log.warn("Failed to create broker {}: {}", type, e.getMessage());
                log.debug("Detailed error for broker {}", type, e);
            }
        }

        return java.util.Collections.unmodifiableMap(brokers);
    }

    private MessageBroker createActiveMqBroker() {
        throw new UnsupportedOperationException("ActiveMQ broker not implemented yet");
    }

    private MessageBroker createRabbitMqBroker() {
        throw new UnsupportedOperationException("RabbitMQ broker not implemented yet");
    }

    private MessageBroker createKafkaBroker() {
        throw new UnsupportedOperationException("Kafka broker not implemented yet");
    }

    private MessageBroker createWebSocketBroker() {
        throw new UnsupportedOperationException("WebSocket broker not implemented yet");
    }

    /**
     * Проверяет доступность конфигурации для брокера
     */
    public boolean isBrokerConfigured(BrokersType brokerType) {
        try {
            brokerProperties.getPropertiesForBroker(brokerType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
