package com.vyanckus.broker;

import com.vyanckus.dto.ReceivedMessage;

/**
 * Функциональный интерфейс для обработки входящих сообщений от брокеров.
 * Используется в подписках (subscribe).
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * Вызывается, когда приходит новое сообщение от брокера
     * @param message полученное сообщение
     * @paramDestination откуда пришло сообщение (queue, topic)
     */
    void onMessage(ReceivedMessage message, String destination);
}
