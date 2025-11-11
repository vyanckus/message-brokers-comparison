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
 * Health Indicator для мониторинга состояния брокеров сообщений в системе.
 * Интегрируется с Spring Boot Actuator для предоставления информации о здоровье
 * через endpoint /actuator/health.
 *
 * <p><b>Проверяемые аспекты здоровья брокеров:</b>
 * <ul>
 *   <li>Доступность брокеров (наличие конфигурации и Spring бинов)</li>
 *   <li>Состояние подключения к брокерам ({@link MessageBroker#isConnected()})</li>
 *   <li>Общее здоровье брокеров ({@link MessageBroker#isHealthy()})</li>
 *   <li>Возможность создания экземпляров брокеров</li>
 * </ul>
 *
 * <p><b>Возвращаемые статусы здоровья:</b>
 * <ul>
 *   <li><b>UP</b> - все доступные брокеры здоровы и подключены</li>
 *   <li><b>DEGRADED</b> - некоторые брокеры имеют проблемы</li>
 *   <li><b>DOWN</b> - критические проблемы со всеми брокерами</li>
 *   <li><b>UNKNOWN</b> - нет настроенных брокеров</li>
 * </ul>
 *
 * <p><b>Пример вывода в /actuator/health:</b>
 * <pre>
 * {
 *   "status": "DEGRADED",
 *   "details": {
 *     "brokerHealth": {
 *       "kafka": "UP",
 *       "rabbitmq": "DOWN",
 *       "healthyBrokers": 1,
 *       "totalBrokers": 2
 *     }
 *   }
 * }
 * </pre>
 *
 * @see HealthIndicator
 * @see MessageBrokerFactory
 * @see MessageBroker
 */
@Component
public class BrokerHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(BrokerHealthIndicator.class);
    private final MessageBrokerFactory brokerFactory;

    /**
     * Конструктор для инъекции зависимости фабрики брокеров.
     *
     * @param brokerFactory фабрика для создания и управления брокерами
     */
    public BrokerHealthIndicator(MessageBrokerFactory brokerFactory) {
        this.brokerFactory = brokerFactory;
    }

    /**
     * Основной метод проверки здоровья брокеров сообщений.
     * Вызывается автоматически Spring Boot Actuator при запросе /actuator/health.
     *
     * <p><b>Алгоритм проверки:</b>
     * <ol>
     *   <li>Получение списка доступных брокеров через фабрику</li>
     *   <li>Создание экземпляров всех доступных брокеров</li>
     *   <li>Проверка подключения и здоровья каждого брокера</li>
     *   <li>Определение общего статуса на основе статистики</li>
     * </ol>
     *
     * @return объект Health с детализированной информацией о состоянии брокеров
     */
    @Override
    public Health health() {
        try {
            Map<BrokersType, Boolean> availableBrokers = brokerFactory.getAvailableBrokers();
            long availableCount = availableBrokers.values().stream().filter(Boolean::booleanValue).count();

            if (availableCount == 0) {
                return Health.unknown()
                        .withDetail("message", "No brokers available")
                        .withDetail("availableBrokers", availableBrokers)
                        .build();
            }

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
            if (healthyBrokers == totalBrokers && totalBrokers == availableCount) {
                return healthBuilder.build();
            } else {
                return healthBuilder
                        .status("DEGRADED")
                        .withDetail("healthyBrokers", healthyBrokers)
                        .withDetail("totalCreatedBrokers", totalBrokers)
                        .withDetail("totalAvailableBrokers", availableCount)
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
