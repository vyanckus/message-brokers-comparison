package com.vyanckus.exception;

/**
 * Исключение, выбрасываемое при неудачных попытках подключения к брокеру сообщений.
 * Охватывает ошибки установления соединения, аутентификации и проверки активности подключения.
 *
 * <p><b>Типичные сценарии:</b>
 * <ul>
 *   <li>Сетевые проблемы (таймауты, недоступность хоста)</li>
 *   <li>Ошибки аутентификации и авторизации</li>
 *   <li>Некорректная конфигурация подключения</li>
 *   <li>Исчерпание ресурсов соединения</li>
 * </ul>
 *
 * @see com.vyanckus.broker.MessageBroker#connect()
 * @see com.vyanckus.broker.MessageBroker#isConnected()
 */
public class BrokerConnectionException extends RuntimeException {
    public BrokerConnectionException(String message) {
        super(message);
    }

    public BrokerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
