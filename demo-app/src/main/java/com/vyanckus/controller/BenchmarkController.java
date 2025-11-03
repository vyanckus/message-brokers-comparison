package com.vyanckus.controller;

import com.vyanckus.dto.BenchmarkRequest;
import com.vyanckus.dto.BenchmarkResult;
import com.vyanckus.service.BenchmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST контроллер для управления benchmark тестами производительности брокеров.
 *
 * <p>Предоставляет API endpoints для:</p>
 * <ul>
 *   <li>Запуска синхронных benchmark тестов</li>
 *   <li>Запуска асинхронных benchmark тестов</li>
 *   <li>Остановки активных benchmark тестов</li>
 *   <li>Мониторинга статуса benchmark тестов</li>
 *   <li>Получения расширенной статистики</li>
 * </ul>
 *
 * @author vyanckus
 * @version 1.0
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkController.class);

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
        log.info("BenchmarkController initialized");
    }

    /**
     * Запускает синхронный benchmark тест производительности.
     *
     * <p>Тест выполняется в текущем потоке и возвращает результаты по завершении.</p>
     *
     * @param request параметры benchmark теста
     * @return ResponseEntity с результатами тестирования
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBenchmark(@RequestBody BenchmarkRequest request) {
        try {
            log.info("Starting synchronous benchmark for brokers: {}", request.brokers());

            BenchmarkResult result = benchmarkService.runBenchmark(request);

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Benchmark completed successfully",
                    "results", result.results(),
                    "timestamp", result.timestamp(),
                    "totalBrokersTested", result.results().size()
            );

            log.info("Synchronous benchmark completed. Tested {} brokers", result.results().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Synchronous benchmark failed: {}", e.getMessage(), e);
            return createErrorResponse("Benchmark failed: " + e.getMessage());
        }
    }

    /**
     * Запускает асинхронный benchmark тест производительности.
     *
     * <p>Тест выполняется в отдельном потоке и возвращает ID для отслеживания статуса.</p>
     *
     * @param request параметры benchmark теста
     * @return ResponseEntity с ID запущенного benchmark теста
     */
    @PostMapping("/start-async")
    public ResponseEntity<Map<String, Object>> startAsyncBenchmark(@RequestBody BenchmarkRequest request) {
        try {
            log.info("Starting asynchronous benchmark for brokers: {}", request.brokers());

            String benchmarkId = benchmarkService.startAsyncBenchmark(request);

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Asynchronous benchmark started",
                    "benchmarkId", benchmarkId,
                    "brokers", request.brokers(),
                    "messageCount", request.messageCount(),
                    "timestamp", java.time.Instant.now()
            );

            log.info("Asynchronous benchmark started with ID: {}", benchmarkId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start asynchronous benchmark: {}", e.getMessage(), e);
            return createErrorResponse("Failed to start benchmark: " + e.getMessage());
        }
    }

    /**
     * Останавливает активный benchmark тест.
     *
     * @param benchmarkId ID benchmark теста для остановки
     * @return ResponseEntity с результатом остановки
     */
    @PostMapping("/stop/{benchmarkId}")
    public ResponseEntity<Map<String, Object>> stopBenchmark(@PathVariable String benchmarkId) {
        try {
            log.info("Stopping benchmark: {}", benchmarkId);

            boolean stopped = benchmarkService.stopBenchmark(benchmarkId);

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", stopped ? "Benchmark stopped successfully" : "Benchmark could not be stopped",
                    "benchmarkId", benchmarkId,
                    "stopped", stopped,
                    "timestamp", java.time.Instant.now()
            );

            log.info("Benchmark {} stop result: {}", benchmarkId, stopped);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to stop benchmark {}: {}", benchmarkId, e.getMessage(), e);
            return createErrorResponse("Failed to stop benchmark: " + e.getMessage());
        }
    }

    /**
     * Возвращает статус всех активных benchmark тестов.
     *
     * @return ResponseEntity со статусом активных benchmark тестов
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBenchmarksStatus() {
        try {
            Map<String, Boolean> activeBenchmarks = benchmarkService.getActiveBenchmarksStatus();

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "activeBenchmarks", activeBenchmarks,
                    "totalActive", activeBenchmarks.size(),
                    "timestamp", java.time.Instant.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get benchmarks status: {}", e.getMessage(), e);
            return createErrorResponse("Failed to get benchmarks status: " + e.getMessage());
        }
    }

    /**
     * Останавливает все активные benchmark тесты.
     *
     * @return ResponseEntity с результатом остановки
     */
    @PostMapping("/stop-all")
    public ResponseEntity<Map<String, Object>> stopAllBenchmarks() {
        try {
            log.info("Stopping all active benchmarks");

            benchmarkService.stopAllBenchmarks();

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "All benchmarks stopped",
                    "timestamp", java.time.Instant.now()
            );

            log.info("All benchmarks stopped successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to stop all benchmarks: {}", e.getMessage(), e);
            return createErrorResponse("Failed to stop all benchmarks: " + e.getMessage());
        }
    }

    /**
     * Возвращает информацию о benchmark сервисе.
     *
     * @return ResponseEntity с информацией о сервисе
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getBenchmarkInfo() {
        Map<String, Object> info = Map.of(
                "status", "SUCCESS",
                "service", "Benchmark Service",
                "description", "Performance testing service for message brokers",
                "features", java.util.List.of(
                        "Synchronous benchmark testing",
                        "Asynchronous benchmark testing",
                        "Multiple broker comparison",
                        "Throughput measurement",
                        "Success rate calculation"
                ),
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.ok(info);
    }

    /**
     * Создает стандартный ответ с ошибкой.
     *
     * @param errorMessage сообщение об ошибке
     * @return ResponseEntity с ошибкой
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = Map.of(
                "status", "ERROR",
                "message", errorMessage,
                "timestamp", java.time.Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}