package com.vyanckus.factory;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.config.BrokerProperties;
import com.vyanckus.dto.BrokersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика для создания экземпляров брокеров сообщений.
 * Использует Spring DI для получения уже созданных бинов.
 * Паттерн Factory скрывает сложность создания разных типов брокеров.
 */
@Component
public class MessageBrokerFactory {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerFactory.class);

    private final ApplicationContext applicationContext;
    private final BrokerProperties brokerProperties;

    // Кэш созданных брокеров
    private final Map<BrokersType, MessageBroker> brokersCache = new HashMap<>();

    // Имена бинов для каждого типа брокера
    private final Map<BrokersType, String> brokerBeanNames = Map.of(
            BrokersType.ACTIVEMQ, "activeMQBroker",
            BrokersType.RABBITMQ, "rabbitMQBroker",
            BrokersType.KAFKA, "kafkaBroker",
            BrokersType.WEBSOCKET, "webSocketBroker"
    );

    public MessageBrokerFactory(ApplicationContext applicationContext,
                                BrokerProperties brokerProperties) {
        this.applicationContext = applicationContext;
        this.brokerProperties = brokerProperties;
        log.info("MessageBrokerFactory initialized");
    }

    /**
     * Создает брокер указанного типа
     * @param brokerType тип брокера
     * @return реализацию MessageBroker
     */
    public MessageBroker createBroker(BrokersType brokerType) {
        // Проверяем кэш
        if (brokersCache.containsKey(brokerType)) {
            return brokersCache.get(brokerType);
        }

        // Проверяем конфигурацию
        if (!isBrokerConfigured(brokerType)) {
            log.warn("Broker {} is not configured properly", brokerType);
            return null;
        }

        try {
            String beanName = brokerBeanNames.get(brokerType);
            MessageBroker broker = (MessageBroker) applicationContext.getBean(beanName);

            // Кэшируем брокер
            brokersCache.put(brokerType, broker);
            log.info("Successfully created broker: {}", brokerType);

            return broker;

        } catch (Exception e) {
            log.error("Failed to create broker {}: {}", brokerType, e.getMessage());
            return null;
        }
    }

    /**
     * Создает все доступные брокеры
     * @return мапа брокеров по типу
     */
    public Map<BrokersType, MessageBroker> createAllBrokers() {
        Map<BrokersType, MessageBroker> brokers = new HashMap<>();

        for (BrokersType type : BrokersType.values()) {
            MessageBroker broker = createBroker(type);
            if (broker != null) {
                brokers.put(type, broker);
            }
        }

        log.info("Created {} out of {} brokers", brokers.size(), BrokersType.values().length);
        return Collections.unmodifiableMap(brokers);
    }

    /**
     * Проверяет доступность конфигурации для брокера
     */
    public boolean isBrokerConfigured(BrokersType brokerType) {
        try {
            // Простая проверка - если есть конфигурация, считаем что брокер настроен
            brokerProperties.getPropertiesForBroker(brokerType);
            return true;
        } catch (Exception e) {
            log.debug("Broker {} not configured: {}", brokerType, e.getMessage());
            return false;
        }
    }

    /**
     * Очищает кэш брокеров
     */
    public void clearCache() {
        brokersCache.clear();
        log.debug("Broker cache cleared");
    }
}
