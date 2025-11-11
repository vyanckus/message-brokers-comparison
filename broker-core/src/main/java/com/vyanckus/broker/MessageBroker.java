package com.vyanckus.broker;

import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.exception.SubscriptionException;

/**
 * Унифицированный интерфейс для всех брокеров сообщений в системе.
 * Определяет стандартный контракт для операций подключения, отправки сообщений,
 * подписки и управления соединением с различными брокерами.
 *
 * <p><b>Типичный lifecycle использования:</b>
 * <ol>
 *   <li>{@link #connect()} - установление соединения</li>
 *   <li>{@link #isConnected()} - проверка состояния</li>
 *   <li>{@link #sendMessage(MessageRequest)} / {@link #subscribe(String, MessageListener)} - операции</li>
 *   <li>{@link #unsubscribe(String)} / {@link #unsubscribeAll()} - отписка когда не нужно</li>
 *   <li>{@link #disconnect()} - закрытие соединения</li>
 * </ol>
 *
 * @see MessageListener
 * @see MessageRequest
 * @see MessageResponse
 */
public interface MessageBroker {

    /**
     * Устанавливает соединение с брокером сообщений.
     *
     * @throws BrokerConnectionException если подключение не удалось
     */
    void connect() throws BrokerConnectionException;

    /**
     * Отправляет сообщение через брокер.
     *
     * @param request DTO с данными сообщения
     * @return Response с результатом отправки
     * @throws MessageSendException если отправка не удалась
     */
    MessageResponse sendMessage(MessageRequest request) throws MessageSendException;

    /**
     * Подписывается на получение сообщений из указанной очереди/топика.
     *
     * @param destination очередь/топик для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если подписка не удалась
     */
    void subscribe(String destination, MessageListener listener) throws SubscriptionException;

    /**
     * Отписывается от получения сообщений из указанной очереди/топика.
     * Освобождает ресурсы, связанные с подпиской.
     *
     * @param destination очередь/топик от которого отписываемся
     * @throws SubscriptionException если отписка не удалась
     */
    void unsubscribe(String destination) throws SubscriptionException;

    /**
     * Отписывается от всех активных подписок.
     * Освобождает все ресурсы, связанные с подписками.
     * Default реализация пустая - брокеры могут переопределить для оптимизации.
     *
     * @throws SubscriptionException если отписка не удалась
     */
    default void unsubscribeAll() throws SubscriptionException {
        // Базовая реализация - ничего не делает
        // Конкретные брокеры могут переопределить для оптимизации
    }

    /**
     * Закрывает соединение с брокером и освобождает все ресурсы.
     * Должен вызывать {@link #unsubscribeAll()} для очистки подписок.
     */
    void disconnect();

    /**
     * Проверяет активность соединения с брокером.
     *
     * @return true если подключение активно, иначе false
     */
    boolean isConnected();

    /**
     * Возвращает тип брокера для идентификации.
     *
     * @return тип брокера
     */
    BrokersType getBrokerType();

    /**
     * Проверяет работоспособность брокера.
     * Включает проверку соединения и базовой функциональности.
     *
     * @return true если брокер работает корректно, иначе false
     */
    default boolean isHealthy() {
        return isConnected();
    }
}
