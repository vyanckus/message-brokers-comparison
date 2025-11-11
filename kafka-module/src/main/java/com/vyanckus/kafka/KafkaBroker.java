package com.vyanckus.kafka;

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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реализация {@link MessageBroker} для Apache Kafka.
 * Использует Kafka Producer/Consumer API для высокопроизводительной обработки сообщений.
 * Оптимизирован для демонстрационных целей с балансом производительности и надежности.
 *
 * <p><b>Особенности реализации:</b>
 * <ul>
 *   <li>Fire-and-forget отправка для максимальной производительности</li>
 *   <li>Thread-per-topic consumption с graceful shutdown</li>
 *   <li>Базовая конфигурация для локального тестирования</li>
 *   <li>Автоматическое создание топиков при первом использовании</li>
 * </ul>
 *
 * @see MessageBroker
 * @see BrokersType#KAFKA
 */
@Component
public class KafkaBroker implements MessageBroker {

    private static final Logger log = LoggerFactory.getLogger(KafkaBroker.class);

    /**
     * Конфигурация Kafka из application.yml.
     */
    private final BrokerProperties.KafkaProperties config;

    /**
     * Kafka Producer для отправки сообщений.
     */
    private KafkaProducer<String, String> producer;

    /**
     * Активные Kafka Consumers для управления подписками.
     */
    private final ConcurrentMap<String, KafkaConsumer<String, String>> activeConsumers = new ConcurrentHashMap<>();

    /**
     * Флаги активности Consumer потоков.
     */
    private final ConcurrentMap<String, Boolean> consumerRunningFlags = new ConcurrentHashMap<>();

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param brokerProperties конфигурация всех брокеров из application.yml
     */
    public KafkaBroker(BrokerProperties brokerProperties) {
        this.config = Optional.ofNullable(brokerProperties.kafka())
                .orElse(new BrokerProperties.KafkaProperties(
                        "localhost:9092", "test-topic", "test-group", 10000, 1000
                ));
        log.info("Kafka broker initialized for: {}", config.bootstrapServers());
    }

    /**
     * Устанавливает соединение с Kafka кластером.
     * Создает Kafka Producer с оптимизированными настройками для демо.
     *
     * @throws BrokerConnectionException если не удалось установить соединение
     */
    @Override
    public void connect() throws BrokerConnectionException {
        if (isConnected()) {
            log.debug("Already connected to Kafka");
            return;
        }

        try {
            log.info("Connecting to Kafka at: {}", config.bootstrapServers());

            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

            // Оптимизированные настройки для демо
            props.put(ProducerConfig.ACKS_CONFIG, "1");           // Подтверждение от лидера
            props.put(ProducerConfig.LINGER_MS_CONFIG, 5);        // Небольшой батчинг
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);   // 16KB батчи
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB буфер
            props.put(ProducerConfig.RETRIES_CONFIG, 2);          // 2 попытки при ошибках

            producer = new KafkaProducer<>(props);
            log.info("Successfully connected to Kafka");

        } catch (Exception e) {
            String errorMessage = "Failed to connect to Kafka at " + config.bootstrapServers();
            log.error("{}: {}", errorMessage, e.getMessage());
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанный топик Kafka.
     * Использует fire-and-forget подход для максимальной производительности в демо.
     *
     * @param request запрос на отправку сообщения
     * @return ответ с результатом отправки
     * @throws MessageSendException если не удалось отправить сообщение
     */
    @Override
    public MessageResponse sendMessage(MessageRequest request) throws MessageSendException {
        if (!isConnected()) {
            throw new MessageSendException("Not connected to Kafka. Call connect() first.");
        }

        try {
            String topicName = request.destination() != null ? request.destination() : config.topic();

            // Простая отправка без блокировки
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, request.message());
            producer.send(producerRecord);

            // Простой ID для демо
            String messageId = "kafka-" + System.currentTimeMillis();

            return MessageResponse.success(BrokersType.KAFKA, messageId);

        } catch (Exception e) {
            log.error("Failed to send message to Kafka: {}", e.getMessage());
            throw new MessageSendException("Failed to send message to Kafka", e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанного топика Kafka.
     * Запускает отдельный поток для потребления сообщений.
     *
     * @param topic топик для подписки
     * @param listener обработчик входящих сообщений
     * @throws SubscriptionException если не удалось подписаться на топик
     */
    @Override
    public void subscribe(String topic, MessageListener listener) throws SubscriptionException {
        if (!isConnected()) {
            throw new SubscriptionException("Not connected to Kafka. Call connect() first.");
        }

        if (activeConsumers.containsKey(topic)) {
            log.warn("Already subscribed to topic: {}", topic);
            return;
        }

        try {
            log.info("Subscribing to topic: {}", topic);

            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-group-" + topic);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(topic));

            activeConsumers.put(topic, consumer);
            consumerRunningFlags.put(topic, true);

            startConsumer(topic, consumer, listener);

            log.info("Successfully subscribed to topic: {}", topic);

        } catch (Exception e) {
            log.error("Failed to subscribe to topic {}: {}", topic, e.getMessage());
            throw new SubscriptionException("Failed to subscribe to topic: " + topic, e);
        }
    }

    /**
     * Запускает consumer для топика в отдельном потоке.
     *
     * @param topic топик для потребления
     * @param consumer Kafka Consumer
     * @param listener обработчик сообщений
     */
    private void startConsumer(String topic, KafkaConsumer<String, String> consumer, MessageListener listener) {
        Thread consumerThread = new Thread(() -> {
            log.info("Consumer started for topic: {}", topic);

            try {
                while (consumerRunningFlags.getOrDefault(topic, false)) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        handleIncomingMessage(record, topic, listener);
                    }
                }
            } catch (Exception e) {
                log.error("Consumer error for topic {}: {}", topic, e.getMessage());
            } finally {
                consumer.close();
                log.info("Consumer stopped for topic: {}", topic);
            }
        });

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    /**
     * Обрабатывает входящее сообщение Kafka.
     *
     * @param consumerRecord входящая запись Kafka
     * @param topic топик-источник сообщения
     * @param listener обработчик сообщений
     */
    private void handleIncomingMessage(ConsumerRecord<String, String> consumerRecord, String topic, MessageListener listener) {
        try {
            ReceivedMessage receivedMessage = new ReceivedMessage(
                    BrokersType.KAFKA,
                    topic,
                    consumerRecord.value(),
                    "kafka-" + consumerRecord.offset(),
                    java.time.Instant.now(),
                    Map.of("partition", consumerRecord.partition(), "offset", consumerRecord.offset())
            );

            listener.onMessage(receivedMessage, topic);

        } catch (Exception e) {
            log.error("Error processing message from topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Отписывается от получения сообщений из указанного топика Kafka.
     *
     * @param topic топик от которого отписываемся
     * @throws SubscriptionException если не удалось отписаться
     */
    @Override
    public void unsubscribe(String topic) throws SubscriptionException {
        consumerRunningFlags.put(topic, false);
        KafkaConsumer<String, String> consumer = activeConsumers.remove(topic);

        if (consumer != null) {
            try {
                consumer.wakeup(); // Пробуждаем consumer из poll()
                log.info("Unsubscribed from topic: {}", topic);
            } catch (Exception e) {
                log.warn("Error unsubscribing from topic {}: {}", topic, e.getMessage());
            }
        }
    }

    /**
     * Отписывается от всех активных подписок Kafka.
     */
    @Override
    public void unsubscribeAll() throws SubscriptionException {
        log.info("Unsubscribing from all Kafka topics...");

        consumerRunningFlags.replaceAll((topic, running) -> false);

        for (String topic : activeConsumers.keySet()) {
            KafkaConsumer<String, String> consumer = activeConsumers.remove(topic);
            if (consumer != null) {
                consumer.wakeup();
            }
        }

        log.info("Unsubscribed from all Kafka topics");
    }

    /**
     * Закрывает соединение с Kafka и освобождает ресурсы.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from Kafka...");

        try {
            unsubscribeAll();
        } catch (Exception e) {
            log.warn("Error during unsubscribe: {}", e.getMessage());
        }

        if (producer != null) {
            producer.close();
            producer = null;
            log.debug("Kafka Producer closed");
        }

        log.info("Successfully disconnected from Kafka");
    }

    /**
     * Проверяет наличие активного соединения с Kafka.
     */
    @Override
    public boolean isConnected() {
        return producer != null;
    }

    /**
     * Возвращает тип брокера - Kafka.
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.KAFKA;
    }

    /**
     * Проверяет работоспособность брокера.
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }
}
