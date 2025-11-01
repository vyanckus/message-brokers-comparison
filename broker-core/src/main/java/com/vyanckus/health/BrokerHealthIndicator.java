package com.vyanckus.health;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.factory.MessageBrokerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health Check для мониторинга состояния всех брокеров сообщений.
 * Автоматически регистрируется в Spring Actuator.
 */
@Component
public class BrokerHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(BrokerHealthIndicator.class);

    private final MessageBrokerFactory brokerFactory;
    private Map<BrokersType, MessageBroker> brokers;

    public BrokerHealthIndicator(MessageBrokerFactory brokerFactory) {
        this.brokerFactory = brokerFactory;
        initializeBrokers();
    }

    /**
     * Инициализация брокеров (ленивая загрузка)
     */
    private void initializeBrokers() {
        try {
            this.brokers = brokerFactory.createAllBrokers();
        } catch (Exception e) {
            this.brokers = Map.of();
            log.error("Failed to initialize brokers: {}", e.getMessage(), e);
        }
    }

    @Override
    public Health health() {
        if (brokers.isEmpty()) {
            return Health.down()
                    .withDetail("error", "No brokers configured or initialization failed")
                    .build();
        }

        Health.Builder healthBuilder = Health.up();
        int healthyBrokers = 0;
        int totalBrokers = brokers.size();

        for (Map.Entry<BrokersType, MessageBroker> entry : brokers.entrySet()) {
            BrokersType brokerType = entry.getKey();
            MessageBroker broker = entry.getValue();

            try {
                boolean isHealthy = broker.isHealthy() && broker.isConnected();

                if (isHealthy) {
                    healthyBrokers++;
                    healthBuilder.withDetail(brokerType.name().toLowerCase(), "UP");
                } else {
                    healthBuilder.withDetail(brokerType.name().toLowerCase(), "DOWN - Not connected");
                }

            } catch (Exception e) {
                healthBuilder.withDetail(brokerType.name().toLowerCase(),
                        "DOWN - " + e.getMessage());
            }
        }

        if (healthyBrokers == totalBrokers) {
            return healthBuilder.build();
        }

        return healthBuilder
                .status("DEGRADED")
                .withDetail("healthyBrokers", healthyBrokers)
                .withDetail("totalBrokers", totalBrokers)
                .build();
    }

    /**
     * Получить статус конкретного брокера
     */
    public Health getBrokerHealth(BrokersType brokerType) {
        MessageBroker broker = brokers.get(brokerType);

        if (broker == null) {
            return Health.down()
                    .withDetail("error", "Broker not found: " + brokerType)
                    .build();
        }

        try {
            if (broker.isHealthy() && broker.isConnected()) {
                return Health.up()
                        .withDetail("brokerType", brokerType)
                        .withDetail("connected", true)
                        .build();
            } else {
                return Health.down()
                        .withDetail("brokerType", brokerType)
                        .withDetail("connected", false)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("brokerType", brokerType)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
