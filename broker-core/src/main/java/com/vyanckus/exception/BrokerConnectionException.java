package com.vyanckus.exception;

/**
 * Исключение при проблемах подключения к брокеру
 */
public class BrokerConnectionException extends RuntimeException {
    public BrokerConnectionException(String message) {
        super(message);
    }

    public BrokerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
