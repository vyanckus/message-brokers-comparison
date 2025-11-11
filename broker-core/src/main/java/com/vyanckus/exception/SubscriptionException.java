package com.vyanckus.exception;

import com.vyanckus.broker.MessageListener;

/**
 * Исключение, выбрасываемое при неудачных попытках подписки на получение сообщений от брокера.
 * Охватывает ошибки создания потребителей и регистрации обработчиков сообщений.
 *
 * <p><b>Типичные сценарии:</b>
 * <ul>
 *   <li>Подписка на несуществующее назначение</li>
 *   <li>Конфликтующие подписки</li>
 *   <li>Отсутствие прав на чтение из назначения</li>
 *   <li>Достижение лимитов подписок</li>
 * </ul>
 *
 * @see com.vyanckus.broker.MessageBroker#subscribe(String, MessageListener)
 */
public class SubscriptionException extends RuntimeException {
    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
