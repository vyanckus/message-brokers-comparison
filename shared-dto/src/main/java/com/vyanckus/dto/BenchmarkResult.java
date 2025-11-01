package com.vyanckus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * DTO для результатов benchmark тестирования одного брокера
 */
public record BenchmarkResult(
        List<BrokerBenchmarkResult> results,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp
) {
    public BenchmarkResult(List<BrokerBenchmarkResult> results) {
        this(results, Instant.now());
    }
}
