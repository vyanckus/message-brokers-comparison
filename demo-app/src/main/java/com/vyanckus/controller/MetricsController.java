package com.vyanckus.controller;

import com.vyanckus.metrics.BrokerMetrics;
import com.vyanckus.dto.BrokersType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST контроллер для предоставления метрик брокеров сообщений и системы.
 *
 * <p>Предоставляет endpoints для получения:</p>
 * <ul>
 *   <li>Метрик производительности брокеров сообщений</li>
 *   <li>Системных метрик JVM и памяти</li>
 *   <li>Статистики отправленных и полученных сообщений</li>
 * </ul>
 *
 * @see BrokerMetrics
 * @see BrokersType
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final BrokerMetrics brokerMetrics;

    public MetricsController(BrokerMetrics brokerMetrics) {
        this.brokerMetrics = brokerMetrics;
    }

    /**
     * Возвращает метрики производительности всех брокеров сообщений.
     *
     * <p>Содержит информацию о:</p>
     * <ul>
     *   <li>Количестве отправленных сообщений</li>
     *   <li>Количестве полученных сообщений</li>
     *   <li>Пропускной способности (throughput)</li>
     *   <li>Статусе активности брокера</li>
     * </ul>
     *
     * @return ResponseEntity с метриками брокеров в формате JSON
     */
    @GetMapping("/brokers")
    public ResponseEntity<Map<String, Object>> getBrokersMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Map<String, Object> brokerMetrics = new HashMap<>();

        for (BrokersType brokerType : BrokersType.values()) {
            Map<String, Object> brokerData = new HashMap<>();

            long sentMessages = this.brokerMetrics.getSentMessageCount(brokerType);
            long receivedMessages = this.brokerMetrics.getReceivedMessageCount(brokerType);

            brokerData.put("messagesSent", sentMessages);
            brokerData.put("messagesReceived", receivedMessages);
            brokerData.put("throughput", calculateThroughput(sentMessages));
            brokerData.put("status", "ACTIVE"); // В реальном приложении проверяли бы подключение

            brokerMetrics.put(brokerType.name().toLowerCase(), brokerData);
        }

        metrics.put("status", "SUCCESS");
        metrics.put("metrics", brokerMetrics);
        metrics.put("timestamp", java.time.Instant.now());

        return ResponseEntity.ok(metrics);
    }

    /**
     * Рассчитывает пропускную способность на основе количества сообщений.
     * В реальном приложении использовались бы временные метки для точного расчета.
     *
     * @param messageCount общее количество отправленных сообщений
     * @return расчетная пропускная способность в сообщениях в секунду
     */
    private double calculateThroughput(long messageCount) {
        // Упрощенный расчет - в реальном приложении использовали бы временные метки
        return messageCount > 0 ? Math.min(200, messageCount * 0.1) : 0;
    }

    /**
     * Возвращает системные метрики JVM и памяти приложения.
     *
     * <p>Содержит информацию о:</p>
     * <ul>
     *   <li>Использовании памяти JVM (heap)</li>
     *   <li>Количестве активных потоков</li>
     *   <li>Доступных процессорах</li>
     *   <li>Общем и использованном объеме памяти</li>
     * </ul>
     *
     * @return ResponseEntity с системными метриками в формате JSON
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();

        // Базовые системные метрики
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // МБ
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // МБ
        long freeMemory = runtime.freeMemory() / (1024 * 1024); // МБ
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;

        systemMetrics.put("memoryUsed", usedMemory);
        systemMetrics.put("memoryTotal", totalMemory);
        systemMetrics.put("memoryMax", maxMemory);
        systemMetrics.put("memoryUsage", Math.round(memoryUsage));
        systemMetrics.put("availableProcessors", runtime.availableProcessors());
        systemMetrics.put("threadCount", Thread.activeCount());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("system", systemMetrics);
        response.put("timestamp", java.time.Instant.now());

        return ResponseEntity.ok(response);
    }
}
