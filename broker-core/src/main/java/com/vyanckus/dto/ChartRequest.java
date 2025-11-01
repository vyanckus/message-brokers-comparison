package com.vyanckus.dto;

/**
 * Запрос на генерацию данных для графика
 */
public record ChartRequest(
        String chartType,
        int dataPoints,
        BrokersType brokerType
) {
    public ChartRequest {
        if (chartType == null) chartType = "line";
        if (dataPoints == 0) dataPoints = 10;
    }
}
