package com.vyanckus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс Spring для активации кастомных свойств брокеров сообщений.
 *
 * <p>Этот класс связывает свойства из application.yml с префиксом "message.broker"
 * с Java record {@link BrokerProperties}, делая их доступными для инъекции в другие компоненты.
 *
 * <p><b>Роль в системе:</b>
 * <ul>
 *   <li>Активирует механизм {@link ConfigurationProperties} для нашего префикса</li>
 *   <li>Делает {@link BrokerProperties} доступным как Spring бин</li>
 *   <li>Обеспечивает типобезопасную конфигурацию брокеров</li>
 * </ul>
 *
 * @see BrokerProperties
 */
@Configuration
@EnableConfigurationProperties(BrokerProperties.class)
public class BrokerConfiguration {
    // Класс не требует дополнительного кода - вся конфигурация
    // выполняется через аннотации Spring Framework
}
