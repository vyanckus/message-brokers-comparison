package com.vyanckus.dto;

/**
 * Data Transfer Object для запроса генерации данных графика.
 * Используется клиентами для указания параметров требуемого графика
 * в WebSocket демонстрации производительности брокеров.
 *
 * <p><b>Параметры настройки графика:</b>
 * <ul>
 *   <li><b>chartType</b> - тип визуализации (линейный, столбчатый, круговая диаграмма)</li>
 *   <li><b>dataPoints</b> - количество точек данных для генерации</li>
 *   <li><b>brokerType</b> - тип брокера для фильтрации данных (опционально)</li>
 * </ul>
 *
 * <p><b>Поддерживаемые типы графиков:</b>
 * <ul>
 *   <li><b>line</b> - линейный график (по умолчанию)</li>
 *   <li><b>bar</b> - столбчатая диаграмма</li>
 *   <li><b>pie</b> - круговая диаграмма</li>
 *   <li><b>radar</b> - радиальная диаграмма</li>
 * </ul>
 *
 * @param chartType тип графика для генерации
 * @param dataPoints количество точек данных для отображения
 * @param brokerType тип брокера для фильтрации данных (может быть null для всех брокеров)
 *
 * @see ChartData
 * @see BrokersType
 */
public record ChartRequest(
        String chartType,
        int dataPoints,
        BrokersType brokerType
) {
    /**
     * Компактный конструктор с установкой значений по умолчанию.
     * Гарантирует, что все обязательные параметры имеют допустимые значения.
     */
    public ChartRequest {
        if (chartType == null) chartType = "line";
        if (dataPoints == 0) dataPoints = 10;
    }
}
