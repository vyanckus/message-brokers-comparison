package com.vyanckus.broker;

import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.exception.SubscriptionException;

/**
 * Унифицированный интерфейс для всех брокеров сообщений.
 * Каждая реализация брокера (ActiveMQ, RabbitMQ, Kafka, WebSocket)
 * должна реализовывать этот интерфейс.
 */
public interface MessageBroker {

    /**
     * Подключение к брокеру сообщений
     * @throws BrokerConnectionException если подключение не удалось
     */
    void connect() throws BrokerConnectionException;

    /**
     * Отправка сообщения в брокер
     * @param request DTO с данными сообщения
     * @return Response с результатом отправки
     * @throws MessageSendException если отправка не удалась
     */
    MessageResponse sendMessage(MessageRequest request) throws MessageSendException;

    /**
     * Подписка на получение сообщений из брокера
     * @param destination очередь/топик на который подписываемся
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если подписка не удалась
     */
    void subscribe(String destination, com.vyanckus.broker.MessageListener listener) throws SubscriptionException;

    /**
     * Отключение от брокера
     */
    void disconnect();

    /**
     * Проверка подключения к брокеру
     * @return true если подключение активно
     */
    boolean isConnected();

    /**
     * Тип брокера (для идентификации)
     */
    BrokersType getBrokerType();

    /**
     * Проверка здоровья брокера (для health checks)
     * @return true если брокер работает корректно
     */
    default boolean isHealthy() {
        return isConnected();
    }
}
