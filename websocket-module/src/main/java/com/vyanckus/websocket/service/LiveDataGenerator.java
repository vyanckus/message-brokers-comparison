package com.vyanckus.websocket.service;

import com.vyanckus.dto.BrokersType;
import com.vyanckus.metrics.BrokerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Сервис для генерации и отправки данных в реальном времени через WebSocket.
 * Отправляет метрики производительности и тестовые данные для визуализации.
 *
 * <p><b>Отправляемые данные:</b>
 * <ul>
 *   <li>Тестовые значения для графиков</li>
 *   <li>Реальные метрики брокеров сообщений</li>
 *   <li>Статистику производительности в реальном времени</li>
 * </ul>
 */
@Service
public class LiveDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(LiveDataGenerator.class);
    private final Random random = new Random();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BrokerMetrics brokerMetrics;

    private boolean generatorRunning = false;
    private Map<String, Object> currentParameters = new HashMap<>();

    /**
     * Запускает генерацию данных для WebSocket демо.
     *
     * @param parameters параметры генерации (interval, amplitude, frequency, dataType)
     */
    public void startGenerator(Map<String, Object> parameters) {
        this.generatorRunning = true;
        this.currentParameters = parameters != null ? parameters : Map.of();
        log.info("Live data generator started with parameters: {}", parameters);
    }

    /**
     * Останавливает генерацию данных.
     */
    public void stopGenerator() {
        this.generatorRunning = false;
        log.info("Live data generator stopped");
    }

    /**
     * Периодическая генерация и отправка данных через WebSocket.
     * Выполняется каждую секунду когда генератор активен.
     */
    @Scheduled(fixedRate = 1000)
    public void generateLiveData() {
        if (!generatorRunning) return;

        try {
            Map<String, Object> liveData = createLiveData();
            messagingTemplate.convertAndSend("/topic/livedata", liveData);
            log.debug("Sent live data via WebSocket");
        } catch (Exception e) {
            log.error("Error generating live data: {}", e.getMessage(), e);
        }
    }

    /**
     * Создает объект с данными для отправки через WebSocket.
     * Включает тестовые данные и реальные метрики брокеров.
     *
     * @return данные для отправки клиенту
     */
    private Map<String, Object> createLiveData() {
        Map<String, Object> data = new HashMap<>();

        // Базовые данные для графиков
        data.put("type", currentParameters.getOrDefault("dataType", "sine"));
        data.put("value", generateValue());
        data.put("timestamp", Instant.now().toString());
        data.put("amplitude", currentParameters.getOrDefault("amplitude", 50));
        data.put("frequency", currentParameters.getOrDefault("frequency", 1));

        // Реальные метрики брокеров
        data.put("brokerMetrics", getRealBrokerMetrics());

        return data;
    }

    /**
     * Генерирует тестовое значение на основе текущих параметров.
     * Имитирует различные типы сигналов как в оригинальном проекте.
     *
     * @return сгенерированное значение
     */
    private double generateValue() {
        String dataType = (String) currentParameters.getOrDefault("dataType", "sine");
        int amplitude = (int) currentParameters.getOrDefault("amplitude", 50);
        int frequency = (int) currentParameters.getOrDefault("frequency", 1);

        long currentTime = System.currentTimeMillis();

        switch (dataType) {
            case "sine":
                return amplitude * Math.sin(2 * Math.PI * frequency * currentTime / 1000.0);
            case "cosine":
                return amplitude * Math.cos(2 * Math.PI * frequency * currentTime / 1000.0);
            case "random":
                return (random.nextDouble() - 0.5) * 2 * amplitude;
            case "sawtooth":
                return 2 * amplitude * ((currentTime / 1000.0 * frequency) % 1) - amplitude;
            default:
                return 50.0 + random.nextInt(25); // Как в оригинальном проекте
        }
    }

    /**
     * Собирает реальные метрики всех брокеров сообщений.
     *
     * @return метрики брокеров
     */
    private Map<String, Object> getRealBrokerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        for (BrokersType broker : BrokersType.values()) {
            metrics.put(broker.name().toLowerCase(), Map.of(
                    "sent", brokerMetrics.getSentMessageCount(broker),
                    "received", brokerMetrics.getReceivedMessageCount(broker),
                    "latency", brokerMetrics.getAverageLatency(broker)
            ));
        }
        return metrics;
    }

    /**
     * Проверяет активен ли генератор данных.
     *
     * @return true если генератор запущен
     */
    public boolean isGeneratorRunning() {
        return generatorRunning;
    }
}
