package com.vyanckus.dto;

import java.util.List;

/**
 * Data Transfer Object для передачи данных графиков в WebSocket демонстрации.
 * Используется для визуализации производительности и метрик брокеров сообщений
 * в реальном времени через WebSocket соединения.
 *
 * <p><b>Структура данных графика:</b>
 * <ul>
 *   <li><b>labels</b> - метки по оси X (временные метки, названия брокеров и т.д.)</li>
 *   <li><b>values</b> - числовые значения по оси Y (количество сообщений, latency и т.д.)</li>
 *   <li><b>chartType</b> - тип графика (line, bar, pie и т.д.)</li>
 *   <li><b>title</b> - заголовок графика для отображения в UI</li>
 * </ul>
 *
 * @param labels список меток для оси X графика
 * @param values список числовых значений для оси Y графика
 * @param chartType тип графика (line, bar, pie, etc.)
 * @param title заголовок графика для отображения
 *
 * @see ChartRequest
 */
public record ChartData(
        List<String> labels,
        List<Number> values,
        String chartType,
        String title
) {
    /**
     * Упрощенный конструктор для создания графика с настройками по умолчанию.
     * Создает линейный график с заголовком "Message Broker Performance".
     *
     * @param labels список меток для оси X
     * @param values список числовых значений для оси Y
     */
    public ChartData(List<String> labels, List<Number> values) {
        this(labels, values, "line", "Message Broker Performance");
    }
}
