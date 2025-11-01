package com.vyanckus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс Spring Boot приложения для сравнения брокеров сообщений.
 *
 * <p>Приложение предоставляет:</p>
 * <ul>
 *   <li>REST API для работы с брокерами сообщений</li>
 *   <li>Web интерфейс для тестирования</li>
 *   <li>Benchmark тестирование производительности</li>
 *   <li>Health checks и мониторинг</li>
 * </ul>
 */
@SpringBootApplication
public class MessageBrokersDemoApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(MessageBrokersDemoApplication.class);
    }
}