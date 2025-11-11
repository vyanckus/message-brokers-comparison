package com.vyanckus.metrics;

import com.vyanckus.dto.BrokersType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Компонент для сбора и управления метриками брокеров сообщений.
 * Интегрируется с Micrometer для предоставления метрик в различных системах мониторинга
 * (Prometheus, Grafana, Spring Boot Actuator, etc.).
 *
 * <p><b>Собираемые метрики:</b>
 * <ul>
 *   <li><b>broker.messages.sent</b> - счетчик отправленных сообщений по типам брокеров</li>
 *   <li><b>broker.messages.received</b> - счетчик полученных сообщений по типам брокеров</li>
 *   <li><b>broker.message.duration</b> - таймер длительности обработки сообщений</li>
 * </ul>
 *
 * <p><b>Доступ через Spring Boot Actuator:</b>
 * <ul>
 *   <li>/actuator/metrics/broker.messages.sent</li>
 *   <li>/actuator/metrics/broker.messages.received</li>
 *   <li>/actuator/metrics/broker.message.duration</li>
 * </ul>
 */
@Component
public class BrokerMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<BrokersType, Counter> sentMessageCounters;
    private final Map<BrokersType, Counter> receivedMessageCounters;
    private final Map<BrokersType, Timer> messageTimer;

    /**
     * Конструктор для инициализации компонента метрик.
     * Автоматически создает все необходимые метрики для каждого типа брокера.
     *
     * @param meterRegistry реестр метрик Micrometer (инжектируется Spring'ом)
     */
    public BrokerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.sentMessageCounters = new ConcurrentHashMap<>();
        this.receivedMessageCounters = new ConcurrentHashMap<>();
        this.messageTimer = new ConcurrentHashMap<>();

        initializeMetrics();
    }

    /**
     * Инициализирует метрики для всех поддерживаемых типов брокеров.
     * Создает отдельные счетчики и таймеры для каждого брокера.
     */
    private void initializeMetrics() {
        for (BrokersType brokerType : BrokersType.values()) {
            String brokerName = brokerType.name().toLowerCase();

            // Счетчик отправленных сообщений
            sentMessageCounters.put(brokerType,
                    Counter.builder("broker.messages.sent")
                            .tag("broker", brokerName)
                            .description("Total messages sent via " + brokerType.getDisplayName())
                            .register(meterRegistry));

            // Счетчик полученных сообщений
            receivedMessageCounters.put(brokerType,
                    Counter.builder("broker.messages.received")
                            .tag("broker", brokerName)
                            .description("Total messages received via " + brokerType.getDisplayName())
                            .register(meterRegistry));

            // Таймер длительности обработки сообщений
            messageTimer.put(brokerType,
                    Timer.builder("broker.message.duration")
                            .tag("broker", brokerName)
                            .description("Message processing duration for " + brokerType.getDisplayName())
                            .register(meterRegistry));
        }
    }

    /**
     * Увеличивает счетчик отправленных сообщений для указанного брокера.
     *
     * @param brokerType тип брокера, через который было отправлено сообщение
     */
    public void incrementSentMessages(BrokersType brokerType) {
        Counter counter = sentMessageCounters.get(brokerType);
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * Увеличивает счетчик полученных сообщений для указанного брокера.
     *
     * @param brokerType тип брокера, через который было получено сообщение
     */
    public void incrementReceivedMessages(BrokersType brokerType) {
        Counter counter = receivedMessageCounters.get(brokerType);
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * Начинает измерение времени обработки сообщения.
     * Возвращает Sample, который должен быть передан в {@link #stopMessageTimer(Timer.Sample, BrokersType)}.
     *
     * @return Timer.Sample для последующего измерения длительности
     */
    public Timer.Sample startMessageTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Останавливает измерение времени обработки сообщения и записывает результат.
     *
     * @param sample образец таймера, полученный из {@link #startMessageTimer()}
     * @param brokerType тип брокера, для которого измеряется время обработки
     */
    public void stopMessageTimer(Timer.Sample sample, BrokersType brokerType) {
        Timer timer = messageTimer.get(brokerType);
        if (timer != null && sample != null) {
            sample.stop(timer);
        }
    }

    /**
     * Возвращает количество отправленных сообщений для указанного брокера.
     *
     * @param brokerType тип брокера
     * @return количество отправленных сообщений (0 если брокер не найден)
     */
    public long getSentMessageCount(BrokersType brokerType) {
        Counter counter = sentMessageCounters.get(brokerType);
        return counter != null ? (long) counter.count() : 0;
    }

    /**
     * Возвращает количество полученных сообщений для указанного брокера.
     *
     * @param brokerType тип брокера
     * @return количество полученных сообщений (0 если брокер не найден)
     */
    public long getReceivedMessageCount(BrokersType brokerType) {
        Counter counter = receivedMessageCounters.get(brokerType);
        return counter != null ? (long) counter.count() : 0;
    }

    public double getAverageLatency(BrokersType brokerType) {
        Timer timer = messageTimer.get(brokerType);
        if (timer != null && timer.count() > 0) {
            return timer.mean(TimeUnit.MILLISECONDS);
        }
        return 0.0;
    }
}
