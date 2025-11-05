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

    public BrokerHealthIndicator(MessageBrokerFactory brokerFactory) {
        this.brokerFactory = brokerFactory;
    }

    @Override
    public Health health() {
        try {
            Map<BrokersType, MessageBroker> brokers = brokerFactory.createAllBrokers();

            if (brokers.isEmpty()) {
                return Health.unknown()
                        .withDetail("message", "No brokers configured")
                        .withDetail("timestamp", java.time.Instant.now())
                        .build();
            }

            Health.Builder healthBuilder = Health.up();
            int healthyBrokers = 0;
            int totalBrokers = brokers.size();

            for (Map.Entry<BrokersType, MessageBroker> entry : brokers.entrySet()) {
                BrokersType brokerType = entry.getKey();
                MessageBroker broker = entry.getValue();

                try {
                    if (broker != null && broker.isConnected() && broker.isHealthy()) {
                        healthyBrokers++;
                        healthBuilder.withDetail(brokerType.name().toLowerCase(), "UP");
                    } else {
                        healthBuilder.withDetail(brokerType.name().toLowerCase(), "DOWN");
                    }
                } catch (Exception e) {
                    healthBuilder.withDetail(brokerType.name().toLowerCase(), "ERROR: " + e.getMessage());
                }
            }

            // Если все брокеры здоровы - UP, иначе - DEGRADED
            if (healthyBrokers == totalBrokers) {
                return healthBuilder.build();
            } else {
                return healthBuilder
                        .status("DEGRADED")
                        .withDetail("healthyBrokers", healthyBrokers)
                        .withDetail("totalBrokers", totalBrokers)
                        .build();
            }

        } catch (Exception e) {
            log.warn("Error checking broker health: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", "Health check failed: " + e.getMessage())
                    .build();
        }
    }
}
