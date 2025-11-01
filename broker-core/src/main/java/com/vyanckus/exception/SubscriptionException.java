package com.vyanckus.exception;

/**
 * Исключение при проблемах подписки на сообщения
 */
public class SubscriptionException extends RuntimeException {
    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
