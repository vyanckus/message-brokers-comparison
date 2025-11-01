package com.vyanckus.activemq;

import com.vyanckus.broker.MessageBroker;
import com.vyanckus.broker.MessageListener;
import com.vyanckus.config.BrokerProperties;
import com.vyanckus.dto.BrokersType;
import com.vyanckus.dto.MessageRequest;
import com.vyanckus.dto.MessageResponse;
import com.vyanckus.dto.ReceivedMessage;
import com.vyanckus.exception.BrokerConnectionException;
import com.vyanckus.exception.MessageSendException;
import com.vyanckus.exception.SubscriptionException;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Реализация {@link MessageBroker} для Apache ActiveMQ.
 * Использует JMS API для отправки и получения сообщений через ActiveMQ брокер.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Подключение к ActiveMQ брокеру</li>
 *   <li>Отправка текстовых сообщений в очереди</li>
 *   <li>Подписка на получение сообщений из очередей</li>
 *   <li>Управление соединением и ресурсами</li>
 * </ul>
 *
 * @author vyanckus
 * @version 1.0
 * @see MessageBroker
 * @see BrokersType#ACTIVEMQ
 */
@Component
public class ActiveMQBroker implements MessageBroker {

    /**
     * Логгер для записи событий и ошибок.
     * Использование SLF4J - стандарт в коммерческой разработке.
     */
    private static final Logger log = LoggerFactory.getLogger(ActiveMQBroker.class);

    /**
     * Конфигурация ActiveMQ из application.yml.
     * Внедряется через конструктор - принцип Dependency Injection.
     */
    private final BrokerProperties.ActiveMQProperties config;

    /**
     * JMS соединение с ActiveMQ брокером.
     */
    private Connection connection;

    /**
     * JMS сессия для работы с сообщениями.
     */
    private Session session;

    /**
     * Конструктор с внедрением зависимостей.
     * Spring автоматически передаст конфигурацию при создании бина.
     *
     * @param brokerProperties конфигурация всех брокеров из application.yml
     */
    public ActiveMQBroker(BrokerProperties brokerProperties) {
        this.config = brokerProperties.activemq();
        log.info("ActiveMQ broker initialized for URL: {}", config.url());
    }

    /**
     * Устанавливает соединение с ActiveMQ брокером.
     * Создает ConnectionFactory, соединение и сессию для работы с JMS.
     *
     * @throws BrokerConnectionException если не удалось установить соединение
     */
    @Override
    public void connect() throws BrokerConnectionException {
        if (isConnected()) {
            log.debug("Already connected to ActiveMQ");
            return;
        }

        try {
            log.info("Connecting to ActiveMQ at: {}", config.url());

            // Создание фабрики соединений с аутентификацией
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.url());
            connectionFactory.setUserName(config.username());
            connectionFactory.setPassword(config.password());

            // Запрещаем доверять всем пакетам (security risk!)
            connectionFactory.setTrustAllPackages(false);

            // Пустой список = разрешаем только примитивные типы
            connectionFactory.setTrustedPackages(new ArrayList<>());

            // Установка соединения и запуск
            connection = connectionFactory.createConnection();
            connection.start();

            // Создание сессии (не транзакционная, автоматическое подтверждение)
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            log.info("Successfully connected to ActiveMQ");

        } catch (JMSException e) {
            String errorMessage = String.format("Failed to connect to ActiveMQ at %s: %s",
                    config.url(), e.getMessage());
            log.error(errorMessage, e);
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанную очередь ActiveMQ.
     * Если очередь не указана в запросе, используется очередь из конфигурации.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    @Override
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!isConnected()) {
            throw new MessageSendException("Not connected to ActiveMQ. Call connect() first.");
        }

        MessageProducer producer = null;
        try {
            // Используем очередь из запроса или конфигурации по умолчанию
            String destinationName = request.destination() != null ?
                    request.destination() : config.queue();

            log.debug("Sending message to queue: {}", destinationName);

            // Создание очереди и продюсера
            Destination destination = session.createQueue(destinationName);
            producer = session.createProducer(destination);

            // Создание текстового сообщения
            TextMessage message = session.createTextMessage(request.message());

            // Отправка сообщения
            producer.send(message);

            log.debug("Message successfully sent to queue: {}", destinationName);

            // Возврат успешного ответа
            return MessageResponse.success(BrokersType.ACTIVEMQ, message.getJMSMessageID());

        } catch (JMSException e) {
            String errorMessage = "Failed to send message to ActiveMQ: " + e.getMessage();
            log.error(errorMessage, e);
            throw new MessageSendException(errorMessage, e);
        } finally {
            // Гарантированное закрытие ресурсов
            closeProducer(producer);
        }
    }

    /**
     * Подписывается на получение сообщений из указанной очереди.
     * Регистрирует MessageListener для асинхронной обработки входящих сообщений.
     *
     * @param destination очередь для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если не удалось подписаться на очередь
     */
    @Override
    public void subscribe(String destination, MessageListener listener) throws SubscriptionException {
        if (!isConnected()) {
            throw new SubscriptionException("Not connected to ActiveMQ. Call connect() first.");
        }

        try {
            log.info("Subscribing to queue: {}", destination);

            // Создание очереди и консьюмера
            Destination jmsDestination = session.createQueue(destination);
            MessageConsumer consumer = session.createConsumer(jmsDestination);

            // Регистрация обработчика сообщений
            consumer.setMessageListener(jmsMessage -> handleIncomingMessage(jmsMessage, destination, listener));

            log.info("Successfully subscribed to queue: {}", destination);

        } catch (JMSException e) {
            String errorMessage = String.format("Failed to subscribe to queue %s: %s",
                    destination, e.getMessage());
            log.error(errorMessage, e);
            throw new SubscriptionException(errorMessage, e);
        }
    }

    /**
     * Обрабатывает входящее JMS сообщение и преобразует его в унифицированный формат.
     *
     * @param jmsMessage входящее JMS сообщение
     * @param destination очередь-источник сообщения
     * @param listener обработчик для уведомления о новом сообщении
     */
    private void handleIncomingMessage(Message jmsMessage, String destination, MessageListener listener) {
        try {
            // Проверяем тип сообщения (работаем только с текстовыми)
            if (!(jmsMessage instanceof TextMessage textMessage)) {
                log.warn("Received non-text message from queue: {}, ignoring", destination);
                return;
            }

            // Извлекаем данные из сообщения
            String messageText = textMessage.getText();
            String messageId = textMessage.getJMSMessageID();

            log.debug("Received message from queue: {} - {}", destination, messageText);

            // Создаем унифицированное представление сообщения
            ReceivedMessage receivedMessage = new ReceivedMessage(
                    BrokersType.ACTIVEMQ,
                    destination,
                    messageText,
                    messageId,
                    java.time.Instant.now(),
                    java.util.Map.of() // Можно добавить дополнительные свойства
            );

            // Уведомляем обработчик
            listener.onMessage(receivedMessage, destination);

        } catch (JMSException e) {
            log.error("Error processing message from queue {}: {}", destination, e.getMessage(), e);
        }
    }

    /**
     * Закрывает соединение с ActiveMQ брокером и освобождает ресурсы.
     * Ресурсы закрываются в правильном порядке: сессия → соединение.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from ActiveMQ...");

        try {
            if (session != null) {
                session.close();
                session = null;
                log.debug("JMS session closed");
            }

            if (connection != null) {
                connection.close();
                connection = null;
                log.debug("JMS connection closed");
            }

            log.info("Successfully disconnected from ActiveMQ");

        } catch (JMSException e) {
            log.warn("Error during disconnect from ActiveMQ: {}", e.getMessage());
        }
    }

    /**
     * Проверяет наличие активного соединения с ActiveMQ.
     *
     * @return true если соединение установлено, иначе false
     */
    @Override
    public boolean isConnected() {
        return session != null;
    }

    /**
     * Возвращает тип брокера - ActiveMQ.
     *
     * @return тип брокера {@link BrokersType#ACTIVEMQ}
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.ACTIVEMQ;
    }

    /**
     * Проверяет работоспособность брокера.
     * В упрощенной реализации проверяет только наличие соединения.
     *
     * @return true если брокер работает корректно, иначе false
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }

    /**
     * Безопасно закрывает JMS MessageProducer с обработкой исключений.
     *
     * @param producer продюсер для закрытия
     */
    private void closeProducer(MessageProducer producer) {
        if (producer != null) {
            try {
                producer.close();
                log.debug("MessageProducer closed successfully");
            } catch (JMSException e) {
                log.debug("Error closing MessageProducer: {}", e.getMessage());
            }
        }
    }
}
