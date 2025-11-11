package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object для агрегированных результатов benchmark-тестирования.
 * Содержит результаты тестирования всех запрошенных брокеров и временную метку выполнения.
 *
 * <p>Используется для возврата комплексных результатов сравнения производительности
 * нескольких брокеров сообщений.
 *
 * <p><b>Структура результатов:</b>
 * <ul>
 *   <li>Список {@link BrokerBenchmarkResult} - результаты для каждого брокера</li>
 *   <li>Временная метка выполнения теста</li>
 *   <li>Возможность сравнения производительности разных брокеров</li>
 * </ul>
 *
 * @param results список результатов тестирования для каждого брокера
 * @param timestamp временная метка выполнения benchmark-теста
 *
 * @see BrokerBenchmarkResult
 * @see BenchmarkRequest
 */
public record BenchmarkResult(

        /**
         * Список результатов benchmark-тестирования для каждого брокера.
         *
         * <p>Порядок элементов соответствует порядку брокеров в исходном
         * {@link BenchmarkRequest#brokers()} запросе.
         *
         * <p>Каждый элемент содержит детальные метрики производительности
         * для соответствующего брокера.
         */
        List<BrokerBenchmarkResult> results,

        /**
         * Временная метка выполнения benchmark-теста.
         *
         * <p>Форматируется в стандарте ISO 8601 в UTC временной зоне.
         * Позволяет отслеживать когда был выполнен тест и сравнивать
         * результаты разных запусков.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp
) {
    /**
     * Создает BenchmarkResult с текущей временной меткой.
     *
     * @param results список результатов тестирования для каждого брокера
     */
    public BenchmarkResult(List<BrokerBenchmarkResult> results) {
        this(results, Instant.now());
    }
}
