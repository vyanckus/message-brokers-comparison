package com.vyanckus.service;

import com.vyanckus.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Сервис для benchmark тестирования производительности брокеров сообщений.
 *
 * <p>Измеряет и сравнивает:</p>
 * <ul>
 *   <li>Скорость отправки сообщений (throughput)</li>
 *   <li>Время отклика (latency)</li>
 *   <li>Успешность доставки</li>
 *   <li>Стабильность работы под нагрузкой</li>
 * </ul>
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private final MessageBrokerService messageBrokerService;

    /**
     * ExecutorService для параллельного выполнения benchmark тестов.
     */
    private final ExecutorService executorService;

    /**
     * Активные benchmark задачи для возможности отмены.
     */
    private final Map<String, Future<?>> activeBenchmarks = new ConcurrentHashMap<>();

    public BenchmarkService(MessageBrokerService messageBrokerService) {
        this.messageBrokerService = messageBrokerService;
        this.executorService = Executors.newCachedThreadPool();
        log.info("BenchmarkService initialized");
    }

    /**
     * Запускает benchmark тестирование для указанных брокеров.
     *
     * @param request параметры benchmark теста
     * @return результаты тестирования
     */
    public BenchmarkResult runBenchmark(BenchmarkRequest request) {
        if (!messageBrokerService.isInitialized()) {
            throw new IllegalStateException("MessageBrokerService not initialized");
        }

        String benchmarkId = UUID.randomUUID().toString();
        log.info("Starting benchmark {} for brokers: {}", benchmarkId, request.brokers());

        List<BrokerBenchmarkResult> results = new ArrayList<>();

        // Запускаем тесты для каждого брокера
        for (BrokersType brokerType : request.brokers()) {
            BrokerBenchmarkResult result = testSingleBroker(brokerType, request);
            results.add(result);
        }

        BenchmarkResult benchmarkResult = new BenchmarkResult(results);

        log.info("Benchmark {} completed. Results: {}", benchmarkId, results.size());

        return benchmarkResult;
    }

    /**
     * Запускает benchmark тестирование в отдельном потоке.
     *
     * @param request параметры benchmark теста
     * @return ID запущенного benchmark
     */
    public String startAsyncBenchmark(BenchmarkRequest request) {
        String benchmarkId = UUID.randomUUID().toString();

        Future<?> future = executorService.submit(() -> {
            try {
                BenchmarkResult result = runBenchmark(request);
                log.info("Async benchmark {} completed with {} results", benchmarkId, result.results().size());
            } catch (Exception e) {
                log.error("Async benchmark {} failed: {}", benchmarkId, e.getMessage(), e);
            } finally {
                activeBenchmarks.remove(benchmarkId);
            }
        });

        activeBenchmarks.put(benchmarkId, future);
        log.info("Async benchmark {} started", benchmarkId);

        return benchmarkId;
    }

    /**
     * Тестирует производительность одного брокера.
     *
     * @param brokerType тип брокера для тестирования
     * @param request параметры теста
     * @return результат тестирования брокера
     */
    private BrokerBenchmarkResult testSingleBroker(BrokersType brokerType, BenchmarkRequest request) {
        log.debug("Testing broker: {}", brokerType);

        Instant startTime = Instant.now();
        int successfulMessages = 0;
        int totalMessages = request.messageCount();

        try {
            // Отправляем указанное количество сообщений
            for (int i = 0; i < totalMessages; i++) {
                if (sendTestMessage(brokerType, request, i)) {
                    successfulMessages++;
                }
            }

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            return createSuccessResult(brokerType, totalMessages, successfulMessages, duration);

        } catch (Exception e) {
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            log.error("Benchmark failed for broker {}: {}", brokerType, e.getMessage(), e);
            return BrokerBenchmarkResult.error(brokerType, e.getMessage());
        }
    }

    /**
     * Отправляет одно тестовое сообщение.
     *
     * @param brokerType тип брокера
     * @param request параметры теста
     * @param messageIndex индекс сообщения
     * @return true если сообщение отправлено успешно, иначе false
     */
    private boolean sendTestMessage(BrokersType brokerType, BenchmarkRequest request, int messageIndex) {
        try {
            MessageRequest messageRequest = new MessageRequest(
                    brokerType,
                    request.destination(),
                    createTestMessage(messageIndex, request)
            );

            MessageResponse response = messageBrokerService.sendMessage(messageRequest);

            return response.isSuccess();

        } catch (Exception e) {
            log.debug("Failed to send test message {} to {}: {}",
                    messageIndex, brokerType, e.getMessage());
            return false;
        }
    }

    /**
     * Создает тестовое сообщение с заданным содержимым.
     *
     * @param index индекс сообщения
     * @param request параметры теста
     * @return текст тестового сообщения
     */
    private String createTestMessage(int index, BenchmarkRequest request) {
        return String.format("Benchmark message #%d at %s", index, Instant.now());
    }

    /**
     * Создает результат успешного тестирования.
     *
     * @param brokerType тип брокера
     * @param totalMessages общее количество сообщений
     * @param successfulMessages количество успешных сообщений
     * @param duration длительность теста
     * @return результат benchmark
     */
    private BrokerBenchmarkResult createSuccessResult(BrokersType brokerType, int totalMessages,
                                                      int successfulMessages, Duration duration) {
        long totalTimeMs = duration.toMillis();
        return BrokerBenchmarkResult.success(brokerType, totalMessages, successfulMessages, totalTimeMs);
    }

    /**
     * Рассчитывает количество сообщений в секунду.
     *
     * @param successfulMessages количество успешных сообщений
     * @param totalTimeMs общее время в миллисекундах
     * @return сообщений в секунду
     */
    private double calculateMessagesPerSecond(int successfulMessages, long totalTimeMs) {
        if (totalTimeMs == 0) {
            return 0;
        }
        return (successfulMessages * 1000.0) / totalTimeMs;
    }

    /**
     * Рассчитывает среднее время на сообщение.
     *
     * @param successfulMessages количество успешных сообщений
     * @param totalTimeMs общее время в миллисекундах
     * @return среднее время на сообщение в миллисекундах
     */
    private double calculateAverageTimePerMessage(int successfulMessages, long totalTimeMs) {
        if (successfulMessages == 0) {
            return 0;
        }
        return (double) totalTimeMs / successfulMessages;
    }

    /**
     * Получает расширенную статистику benchmark тестирования с дополнительными метриками.
     *
     * <p>Возвращает расширенный набор метрик, который может использоваться для:</p>
     * <ul>
     *   <li>Детализированного анализа производительности в UI</li>
     *   <li>Сравнения брокеров по дополнительным параметрам</li>
     *   <li>Генерации отчетов и графиков</li>
     * </ul>
     *
     * <p>Содержит метрики:</p>
     * <ul>
     *   <li><b>brokerType</b> - тип брокера</li>
     *   <li><b>throughput</b> - пропускная способность (сообщений/секунду)</li>
     *   <li><b>successRate</b> - процент успешных сообщений</li>
     *   <li><b>averageLatency</b> - средняя задержка на сообщение (миллисекунды)</li>
     * </ul>
     *
     * @param result результат benchmark тестирования одного брокера
     * @return Map с расширенной статистикой, где ключи - названия метрик,
     *         значения - соответствующие числовые показатели
     *
     * @see BrokerBenchmarkResult
     * @since 1.0
     */
    public Map<String, Object> getExtendedBenchmarkStats(BrokerBenchmarkResult result) {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        stats.put("brokerType", result.brokerType());
        stats.put("throughput", result.messagesPerSecond());
        stats.put("successRate", (double) result.successfulMessages() / result.totalMessages() * 100);
        stats.put("averageLatency", calculateAverageTimePerMessage(result.successfulMessages(), result.totalTimeMs()));

        return stats;
    }

    /**
     * Останавливает запущенный benchmark тест.
     *
     * @param benchmarkId ID benchmark для остановки
     * @return true если benchmark остановлен, иначе false
     */
    public boolean stopBenchmark(String benchmarkId) {
        Future<?> future = activeBenchmarks.get(benchmarkId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            activeBenchmarks.remove(benchmarkId);

            log.info("Benchmark {} {}", benchmarkId, cancelled ? "stopped" : "could not be stopped");
            return cancelled;
        }

        log.warn("Benchmark {} not found", benchmarkId);
        return false;
    }

    /**
     * Возвращает статус всех активных benchmark тестов.
     *
     * @return Map с статусами benchmark тестов
     */
    public Map<String, Boolean> getActiveBenchmarksStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();

        for (Map.Entry<String, Future<?>> entry : activeBenchmarks.entrySet()) {
            status.put(entry.getKey(), !entry.getValue().isDone());
        }

        return status;
    }

    /**
     * Останавливает все активные benchmark тесты.
     */
    public void stopAllBenchmarks() {
        log.info("Stopping all active benchmarks...");

        for (Map.Entry<String, Future<?>> entry : activeBenchmarks.entrySet()) {
            entry.getValue().cancel(true);
            log.debug("Stopped benchmark: {}", entry.getKey());
        }

        activeBenchmarks.clear();
        log.info("All benchmarks stopped");
    }

    /**
     * Освобождает ресурсы сервиса.
     */
    public void shutdown() {
        log.info("Shutting down BenchmarkService...");

        stopAllBenchmarks();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("BenchmarkService shutdown completed");
    }
}
