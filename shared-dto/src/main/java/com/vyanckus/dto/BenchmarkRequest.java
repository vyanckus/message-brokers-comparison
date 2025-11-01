package com.vyanckus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * DTO для запроса benchmark тестирования производительности брокеров
 */
public record BenchmarkRequest(
        @NotNull(message = "Brokers list cannot be null")
        List<BrokersType> brokers,

        @Min(value = 1, message = "Message count must be at least 1")
        int messageCount,

        String destination
) {
    public BenchmarkRequest {
        Objects.requireNonNull(brokers, "brokers cannot be null");
        if (brokers.isEmpty()) {
            throw new IllegalArgumentException("Brokers list cannot be empty");
        }
        if (destination == null || destination.isBlank()) {
            destination = "benchmark-queue";
        }
    }

    // Упрощенный конструктор
    public BenchmarkRequest(List<BrokersType> brokers, int messageCount) {
        this(brokers, messageCount, "benchmark-queue");
    }
}
