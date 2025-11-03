package com.vyanckus.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для активации свойств брокеров.
 * Включает поддержку BrokerProperties из application.yml
 */
@Configuration
@EnableConfigurationProperties(BrokerProperties.class)
public class BrokerConfiguration {
    // Этот класс активирует BrokerProperties для Spring Boot
}