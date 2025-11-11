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
 * Фабрика для создания и управления экземплярами брокеров сообщений.
 * Реализует паттерн Factory для централизованного создания брокеров.
 *
 * <p><b>Основные функции:</b>
 * <ul>
 *   <li>Создание брокеров по типу с проверкой доступности</li>
 *   <li>Кэширование созданных экземпляров для производительности</li>
 *   <li>Проверка конфигурации и наличия Spring бинов</li>
 *   <li>Предоставление информации о доступных брокерах</li>
 * </ul>
 *
 * <p><b>Жизненный цикл брокеров:</b>
 * <ol>
 *   <li>Проверка конфигурации в {@link BrokerProperties}</li>
 *   <li>Проверка наличия Spring бина для брокера</li>
 *   <li>Создание и кэширование экземпляра</li>
 *   <li>Возврат кэшированного экземпляра при повторных запросах</li>
 * </ol>
 */
@Component
public class MessageBrokerFactory {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerFactory.class);

    private final ApplicationContext applicationContext;
    private final BrokerProperties brokerProperties;

    private final Map<BrokersType, MessageBroker> brokersCache = new HashMap<>();

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
     * Создает или возвращает кэшированный брокер указанного типа.
     *
     * @param brokerType тип брокера для создания
     * @return экземпляр MessageBroker или null если брокер недоступен
     */
    public MessageBroker createBroker(BrokersType brokerType) {
        if (brokersCache.containsKey(brokerType)) {
            log.debug("Returning cached broker: {}", brokerType);
            return brokersCache.get(brokerType);
        }

        if (!isBrokerAvailable(brokerType)) {
            log.warn("Broker {} is not available", brokerType);
            return null;
        }

        try {
            String beanName = brokerBeanNames.get(brokerType);
            MessageBroker broker = (MessageBroker) applicationContext.getBean(beanName);
            brokersCache.put(brokerType, broker);
            log.info("Successfully created broker: {}", brokerType);
            return broker;

        } catch (Exception e) {
            log.error("Failed to create broker {}: {}", brokerType, e.getMessage());
            return null;
        }
    }

    /**
     * Проверяет доступность брокера (конфигурация + Spring бин).
     *
     * @param brokerType тип брокера для проверки
     * @return true если брокер настроен и доступен, иначе false
     */
    public boolean isBrokerAvailable(BrokersType brokerType) {
        // 1. Проверяем конфигурацию в BrokerProperties
        if (!brokerProperties.isBrokerConfigured(brokerType)) {
            log.debug("Broker {} is not configured", brokerType);
            return false;
        }

        // 2. Проверяем наличие Spring бина
        String beanName = brokerBeanNames.get(brokerType);
        if (!applicationContext.containsBean(beanName)) {
            log.debug("Bean '{}' not found for broker {}", beanName, brokerType);
            return false;
        }

        return true;
    }

    /**
     * Возвращает информацию о доступных брокерах.
     *
     * @return мапа с флагами доступности для каждого типа брокера
     */
    public Map<BrokersType, Boolean> getAvailableBrokers() {
        Map<BrokersType, Boolean> available = new HashMap<>();
        for (BrokersType type : BrokersType.values()) {
            available.put(type, isBrokerAvailable(type));
        }
        return Collections.unmodifiableMap(available);
    }

    /**
     * Создает все доступные и настроенные брокеры.
     *
     * @return мапа созданных брокеров по типу (только успешно созданные)
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
     * Очищает кэш созданных брокеров.
     * Полезно при переконфигурации или переподключении.
     */
    public void clearCache() {
        brokersCache.clear();
        log.debug("Broker cache cleared");
    }
}
