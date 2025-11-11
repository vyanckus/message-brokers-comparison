package com.vyanckus.broker;

import com.vyanckus.dto.ReceivedMessage;

/**
 * Функциональный интерфейс для обработки входящих сообщений от брокеров.
 * Реализует паттерн Observer для получения уведомлений о новых сообщениях
 * в механизме подписки {@link MessageBroker#subscribe(String, MessageListener)}.
 *
 * <p>Использование в качестве лямбда-выражения:
 * <pre>{@code
 * broker.subscribe("orders", (message, destination) -> {
 *     // обработка сообщения
 *     processOrder(message.message());
 * });
 * }</pre>
 *
 * <p>Использование через method reference:
 * <pre>{@code
 * broker.subscribe("notifications", this::handleNotification);
 * }</pre>
 *
 * @see MessageBroker
 * @see ReceivedMessage
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * Вызывается брокером при получении нового сообщения из указанного назначения.
     * Реализация этого метода должна содержать логику обработки входящего сообщения.
     *
     * @param message полученное сообщение с метаданными и телом
     * @param destination назначение (очередь, топик) откуда пришло сообщение
     *
     * <p><b>Требования к реализации:</b> Реализации должны быть потокобезопасными и обрабатывать исключения
     *           внутри метода, так как брокер не обрабатывает исключения из слушателей.
     */
    void onMessage(ReceivedMessage message, String destination);
}
