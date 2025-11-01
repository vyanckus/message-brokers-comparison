package com.vyanckus.dto;

/**
 * Результат benchmark для одного брокера
 */
public record BrokerBenchmarkResult(
        BrokersType brokerType,
        int totalMessages,
        int successfulMessages,
        long totalTimeMs,
        double messagesPerSecond,
        String status
) {
    public static BrokerBenchmarkResult success(BrokersType brokerType, int total, int successful, long timeMs) {
        double mps = timeMs > 0 ? (successful * 1000.0) / timeMs : 0;
        return new BrokerBenchmarkResult(brokerType, total, successful, timeMs, mps, "SUCCESS");
    }

    public static BrokerBenchmarkResult error(BrokersType brokerType, String error) {
        return new BrokerBenchmarkResult(brokerType, 0, 0, 0, 0, "ERROR: " + error);
    }

    public boolean isSuccess() {
        return status.startsWith("SUCCESS");
    }
}
