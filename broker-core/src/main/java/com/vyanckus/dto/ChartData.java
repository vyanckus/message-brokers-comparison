package com.vyanckus.dto;

import java.util.List;

/**
 * DTO для данных графиков (WebSocket демонстрация)
 */
public record ChartData(
        List<String> labels,
        List<Number> values,
        String chartType,
        String title
) {
    public ChartData(List<String> labels, List<Number> values) {
        this(labels, values, "line", "Message Broker Performance");
    }
}
