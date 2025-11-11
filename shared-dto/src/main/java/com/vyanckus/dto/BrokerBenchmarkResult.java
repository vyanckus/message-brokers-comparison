package com.vyanckus.dto;

/**
 * Результат benchmark-тестирования для одного конкретного брокера сообщений.
 * Содержит метрики производительности и статус выполнения теста.
 *
 * <p>Используется для сравнения производительности разных брокеров и анализа
 * их поведения под нагрузкой.
 *
 * <p><b>Метрики производительности:</b>
 * <ul>
 *   <li>{@code totalMessages} - общее количество отправленных сообщений</li>
 *   <li>{@code successfulMessages} - количество успешно доставленных сообщений</li>
 *   <li>{@code totalTimeMs} - общее время выполнения теста в миллисекундах</li>
 *   <li>{@code messagesPerSecond} - расчетная пропускная способность (сообщений/секунду)</li>
 * </ul>
 *
 * @param brokerType тип брокера, для которого выполнен тест
 * @param totalMessages общее количество отправленных сообщений
 * @param successfulMessages количество успешно доставленных сообщений
 * @param totalTimeMs общее время выполнения теста в миллисекундах
 * @param messagesPerSecond пропускная способность (сообщений в секунду)
 * @param status статус выполнения теста
 *
 * @see BenchmarkResult
 * @see BenchmarkRequest
 */
public record BrokerBenchmarkResult(
        /**
         * Тип брокера, для которого выполнен benchmark-тест.
         * Соответствует одному из элементов из {@link BenchmarkRequest#brokers()}.
         */
        BrokersType brokerType,

        /**
         * Общее количество сообщений, которые пытались отправить в брокер.
         * Соответствует {@link BenchmarkRequest#messageCount()}.
         */
        int totalMessages,

        /**
         * Количество сообщений, успешно доставленных в брокер.
         *
         * <p>Может быть меньше {@code totalMessages} в случае ошибок доставки,
         * таймаутов или проблем с соединением.
         */
        int successfulMessages,

        /**
         * Общее время выполнения теста для данного брокера в миллисекундах.
         *
         * <p>Измеряется от начала отправки первого сообщения до получения
         * подтверждения о доставке последнего сообщения.
         */
        long totalTimeMs,

        /**
         * Расчетная пропускная способность брокера в сообщениях в секунду.
         *
         * <p><b>Формула расчета:</b>
         * {@code messagesPerSecond = (successfulMessages * 1000) / totalTimeMs}
         *
         * <p>Если {@code totalTimeMs = 0}, значение устанавливается в 0 для избежания
         * деления на ноль.
         */
        double messagesPerSecond,

        /**
         * Статус выполнения benchmark-теста для данного брокера.
         *
         * <p><b>Возможные значения:</b>
         * <ul>
         *   <li>{@code "SUCCESS"} - тест завершен успешно</li>
         *   <li>{@code "ERROR: <описание>"} - произошла ошибка во время теста</li>
         * </ul>
         */
        String status
) {
    /**
     * Создает успешный результат benchmark-теста с расчетом метрик производительности.
     *
     * @param brokerType тип брокера
     * @param total общее количество отправленных сообщений
     * @param successful количество успешно доставленных сообщений
     * @param timeMs общее время выполнения теста в миллисекундах
     * @return результат теста со статусом "SUCCESS" и расчетной производительностью
     *
     * @throws ArithmeticException если {@code timeMs = 0} (обрабатывается внутри метода)
     *
     * <p><b>Примечание:</b> Автоматически рассчитывает {@code messagesPerSecond} и устанавливает
     *          статус "SUCCESS". Если {@code timeMs = 0}, {@code messagesPerSecond}
     *          устанавливается в 0.
     */
    public static BrokerBenchmarkResult success(BrokersType brokerType, int total, int successful, long timeMs) {
        double mps = timeMs > 0 ? (successful * 1000.0) / timeMs : 0;
        return new BrokerBenchmarkResult(brokerType, total, successful, timeMs, mps, "SUCCESS");
    }

    /**
     * Создает результат benchmark-теста с ошибкой.
     * Используется когда тест не может быть выполнен из-за проблем с брокером.
     *
     * @param brokerType тип брокера
     * @param error описание ошибки
     * @return результат теста со статусом "ERROR" и нулевыми метриками
     *
     * <p><b>Примечание:</b> Все метрики производительности устанавливаются в 0, так как
     *          тест не был выполнен. Статус формируется как "ERROR: " + описание.
     */
    public static BrokerBenchmarkResult error(BrokersType brokerType, String error) {
        return new BrokerBenchmarkResult(brokerType, 0, 0, 0, 0, "ERROR: " + error);
    }

    /**
     * Проверяет, был ли benchmark-тест для данного брокера успешным.
     *
     * @return {@code true} если статус начинается с "SUCCESS", иначе {@code false}
     *
     * <p><b>Примечание:</b> Использует {@link String#startsWith(String)} для проверки,
     *          поэтому работает для статусов "SUCCESS", "SUCCESS_WITH_WARNINGS" и т.д.
     */
    public boolean isSuccess() {
        return status.startsWith("SUCCESS");
    }
}
