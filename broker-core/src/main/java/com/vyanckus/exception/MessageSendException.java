package com.vyanckus.exception;

import com.vyanckus.dto.MessageRequest;

/**
 * Исключение, выбрасываемое при неудачных попытках отправки сообщения в брокер.
 * Охватывает ошибки маршрутизации, сериализации и доставки сообщений.
 *
 * <p><b>Типичные сценарии:</b>
 * <ul>
 *   <li>Отправка в несуществующее назначение (очередь/топик)</li>
 *   <li>Превышение максимального размера сообщения</li>
 *   <li>Ошибки сериализации содержимого сообщения</li>
 *   <li>Отсутствие прав на запись в назначение</li>
 * </ul>
 *
 * @see com.vyanckus.broker.MessageBroker#sendMessage(MessageRequest)
 */
public class MessageSendException extends RuntimeException {
    public MessageSendException(String message) {
        super(message);
    }

    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
