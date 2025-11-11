package com.vyanckus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object для запроса benchmark тестирования производительности брокеров сообщений.
 * Позволяет сравнивать производительность разных брокеров при различных нагрузках.
 *
 * @param brokers список брокеров для тестирования (порядок сохраняется)
 * @param messageCount количество сообщений для отправки в каждом тесте
 * @param destination назначение для тестовых сообщений
 */
public record BenchmarkRequest(

        /**
         * Список брокеров для тестирования.
         *
         * <p><b>Особенности:</b>
         * <ul>
         *   <li>Должен содержать хотя бы один брокер</li>
         *   <li>Порядок элементов сохраняется (важно для последовательности тестов)</li>
         *   <li>Дубликаты разрешены (для тестирования разных конфигураций одного брокера)</li>
         * </ul>
         */
        @NotNull(message = "Brokers list cannot be null")
        List<BrokersType> brokers,

        /**
         * Количество сообщений для отправки в каждом тесте.
         * Определяет нагрузку теста и влияет на продолжительность выполнения.
         *
         * <p>Минимальное количество сообщений 1.
         */
        @Min(value = 1, message = "Message count must be at least 1")
        int messageCount,

        /**
         * Назначение для тестовых сообщений.
         *
         * <p>Если не указано, используется "benchmark-queue" по умолчанию.
         */
        String destination
) {
    /**
     * Компактный конструктор с расширенной валидацией и значениями по умолчанию.
     *
     * <p><b>Выполняемые проверки:</b>
     * <ul>
     *   <li>Проверка что brokers не {@code null}</li>
     *   <li>Проверка что brokers не пустой</li>
     *   <li>Установка destination по умолчанию если не указан</li>
     * </ul>
     *
     * @throws NullPointerException если brokers = {@code null}
     * @throws IllegalArgumentException если brokers пустой
     */
    public BenchmarkRequest {
        Objects.requireNonNull(brokers, "brokers cannot be null");
        if (brokers.isEmpty()) {
            throw new IllegalArgumentException("Brokers list cannot be empty");
        }
        if (destination == null || destination.isBlank()) {
            destination = "benchmark-queue";
        }
    }

    /**
     * Упрощенный конструктор для создания benchmark запроса с destination по умолчанию.
     *
     * @param brokers список брокеров для тестирования
     * @param messageCount количество сообщений для отправки
     */
    public BenchmarkRequest(List<BrokersType> brokers, int messageCount) {
        this(brokers, messageCount, "benchmark-queue");
    }
}
