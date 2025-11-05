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
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Реализация {@link MessageBroker} для Apache Kafka.
 * Использует Kafka Producer/Consumer API для высокопроизводительной обработки сообщений.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Подключение к Kafka кластеру</li>
 *   <li>Отправка сообщений в топики</li>
 *   <li>Подписка на получение сообщений из топиков</li>
 *   <li>Управление Producer и Consumer инстансами</li>
 * </ul>
 *
 * @author vyanckus
 * @version 1.0
 * @see MessageBroker
 * @see BrokersType#KAFKA
 */
@Component
public class KafkaBroker implements MessageBroker {

    /**
     * Логгер для записи событий и ошибок.
     */
    private static final Logger log = LoggerFactory.getLogger(KafkaBroker.class);

    /**
     * Константы для шаблонов сообщений.
     */
    private static final String LOG_FORMAT = "{}: {}";
    private static final String SEND_ERROR_MSG = "Failed to send message to Kafka";
    private static final String SUBSCRIBE_ERROR_MSG = "Failed to subscribe to topic {}";

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
     * Флаг активности Consumer потоков.
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
        log.info("Kafka broker initialized for bootstrap servers: {}", config.bootstrapServers());
    }

    /**
     * Устанавливает соединение с Kafka кластером.
     * Создает Kafka Producer для отправки сообщений.
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

            // Настройка свойств Producer
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);

            // Создание Producer
            producer = new KafkaProducer<>(props);

            log.info("Successfully connected to Kafka");

        } catch (Exception e) {
            String errorMessage = String.format("Failed to connect to Kafka at %s", config.bootstrapServers());
            log.error(LOG_FORMAT, errorMessage, e.getMessage(), e);
            throw new BrokerConnectionException(errorMessage, e);
        }
    }

    /**
     * Отправляет сообщение в указанный топик Kafka.
     * Если топик не указан в запросе, используется топик из конфигурации.
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

            // ЯВНО СОЗДАЕМ ТОПИК ПЕРЕД ОТПРАВКОЙ
            createTopicIfNotExists(topicName);

            log.debug("Sending message to topic: {}", topicName);

            // Создание записи для отправки
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, request.message());

            // Отправка сообщения и ожидание подтверждения
            Future<RecordMetadata> future = producer.send(producerRecord);
            RecordMetadata metadata = future.get();

            String messageId = String.format("%s-%d-%d",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

            log.debug("Message successfully sent to topic: {} [partition: {}, offset: {}]",
                    topicName, metadata.partition(), metadata.offset());

            return MessageResponse.success(BrokersType.KAFKA, messageId);

        } catch (Exception e) {
            log.error(LOG_FORMAT, SEND_ERROR_MSG, e.getMessage(), e);
            throw new MessageSendException(SEND_ERROR_MSG, e);
        }
    }

    /**
     * Подписывается на получение сообщений из указанного топика Kafka.
     * Запускает отдельный поток для потребления сообщений из топика.
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

            // ПРОСТАЯ КОНФИГУРАЦИЯ ДЛЯ ТЕСТА
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + topic + "-" + System.currentTimeMillis());
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(topic));

            activeConsumers.put(topic, consumer);
            consumerRunningFlags.put(topic, true);

            // ЗАПУСКАЕМ ПРОСТОЙ POLLING
            startSimpleConsumer(topic, consumer, listener);

            log.info("Successfully subscribed to topic: {}", topic);

        } catch (Exception e) {
            log.error("Failed to subscribe to topic {}: {}", topic, e.getMessage(), e);
            throw new SubscriptionException("Failed to subscribe to topic: " + topic, e);
        }
    }

    private void startSimpleConsumer(String topic, KafkaConsumer<String, String> consumer, MessageListener listener) {
        Thread consumerThread = new Thread(() -> {
            log.info("Simple consumer started for topic: {}", topic);

            try {
                while (consumerRunningFlags.getOrDefault(topic, false)) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        log.info("SIMPLE CONSUMER - Received: {}", record.value());
                        handleIncomingMessage(record, topic, listener);
                    }
                }
            } catch (Exception e) {
                log.error("Simple consumer error for topic {}: {}", topic, e.getMessage());
            } finally {
                consumer.close();
                log.info("Simple consumer stopped for topic: {}", topic);
            }
        });

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void createTopicIfNotExists(String topicName) {
        try {
            // Проверяем существование топика через AdminClient
            Properties adminProps = new Properties();
            adminProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());

            try (AdminClient adminClient = AdminClient.create(adminProps)) {
                DescribeTopicsResult describeResult = adminClient.describeTopics(Collections.singleton(topicName));
                describeResult.topicNameValues().get(topicName).get(10, TimeUnit.SECONDS);
                log.debug("Topic {} already exists", topicName);
            } catch (ExecutionException e) {
                // Топик не существует - создаем его
                log.info("Creating topic: {}", topicName);
                try (AdminClient adminClient = AdminClient.create(adminProps)) {
                    NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
                    adminClient.createTopics(Collections.singleton(newTopic)).all().get(10, TimeUnit.SECONDS);
                    log.info("Topic {} created successfully", topicName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not create topic {}: {}", topicName, e.getMessage());
        }
    }

    /**
     * Запускает поток для потребления сообщений из Kafka топика.
     *
     * @param topic топик для потребления
     * @param consumer Kafka Consumer
     * @param listener обработчик сообщений
     */
    private void startConsumerThread(String topic, KafkaConsumer<String, String> consumer, MessageListener listener) {
        Thread consumerThread = new Thread(() -> {
            log.info("Starting consumer thread for topic: {}", topic);

            try {
                while (consumerRunningFlags.getOrDefault(topic, false)) {
                    try {
                        // Опрашиваем Consumer для получения новых сообщений
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000)); // Увеличил timeout

                        if (!records.isEmpty()) {
                            log.debug("Received {} messages from topic: {}", records.count(), topic);

                            for (ConsumerRecord<String, String> consumerRecord : records) {
                                handleIncomingMessage(consumerRecord, topic, listener);
                            }
                        }

                    } catch (Exception e) {
                        if (consumerRunningFlags.getOrDefault(topic, false)) {
                            log.error("Error in consumer thread for topic {}: {}", topic, e.getMessage(), e);
                            // Не прерываем поток при временных ошибках
                            try {
                                Thread.sleep(1000); // Пауза перед повторной попыткой
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            } finally {
                // Гарантированно закрываем Consumer при завершении потока
                closeConsumer(consumer, topic);
                log.info("Consumer thread stopped for topic: {}", topic);
            }
        });

        consumerThread.setName("kafka-consumer-" + topic);
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    /**
     * Обрабатывает входящее сообщение Kafka и преобразует его в унифицированный формат.
     *
     * @param consumerRecord входящая запись Kafka
     * @param topic топик-источник сообщения
     * @param listener обработчик для уведомления о новом сообщении
     */
    private void handleIncomingMessage(ConsumerRecord<String, String> consumerRecord, String topic, MessageListener listener) {
        try {
            String messageText = consumerRecord.value();

            log.debug("Kafka received message - Topic: {}, Partition: {}, Offset: {}",
                    topic, consumerRecord.partition(), consumerRecord.offset());

            Map<String, Object> properties = new HashMap<>();
            properties.put("partition", consumerRecord.partition());
            properties.put("offset", consumerRecord.offset());
            if (consumerRecord.key() != null) {
                properties.put("key", consumerRecord.key());
            }
            properties.put("timestamp", consumerRecord.timestamp());
            properties.put("topic", topic);

            ReceivedMessage receivedMessage = new ReceivedMessage(
                    BrokersType.KAFKA,
                    topic,
                    messageText,
                    String.format("%s-%d-%d", topic, consumerRecord.partition(), consumerRecord.offset()),
                    java.time.Instant.ofEpochMilli(consumerRecord.timestamp()),
                    properties
            );

            listener.onMessage(receivedMessage, topic);
            log.debug("Successfully processed Kafka message from topic: {}", topic);

        } catch (Exception e) {
            log.error("Error processing Kafka message from topic {}: {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Закрывает соединение с Kafka кластером и освобождает ресурсы.
     * Останавливает все Consumer потоки и закрывает Producer.
     */
    @Override
    public void disconnect() {
        log.info("Disconnecting from Kafka...");

        try {
            // Останавливаем все Consumer потоки
            stopAllConsumers();

            if (producer != null) {
                producer.close();
                producer = null;
                log.debug("Kafka Producer closed");
            }

            log.info("Successfully disconnected from Kafka");

        } catch (Exception e) {
            log.warn("Error during disconnect from Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Останавливает все активные Consumer потоки.
     */
    private void stopAllConsumers() {
        // Устанавливаем флаги остановки для всех Consumers
        consumerRunningFlags.replaceAll((topic, running) -> false);

        // Закрываем все Consumers
        for (String topic : activeConsumers.keySet()) {
            KafkaConsumer<String, String> consumer = activeConsumers.remove(topic);
            closeConsumer(consumer, topic);
        }

        consumerRunningFlags.clear();
        log.debug("All Kafka consumers stopped");
    }

    /**
     * Безопасно закрывает Kafka Consumer.
     *
     * @param consumer Consumer для закрытия
     * @param topic топик для логирования
     */
    private void closeConsumer(KafkaConsumer<String, String> consumer, String topic) {
        if (consumer != null) {
            try {
                consumer.close();
                log.debug("Kafka Consumer closed for topic: {}", topic);
            } catch (Exception e) {
                log.debug("Error closing Kafka Consumer for topic {}: {}", topic, e.getMessage());
            }
        }
    }

    /**
     * Проверяет наличие активного соединения с Kafka.
     *
     * @return true если Producer создан, иначе false
     */
    @Override
    public boolean isConnected() {
        return producer != null;
    }

    /**
     * Возвращает тип брокера - Kafka.
     *
     * @return тип брокера {@link BrokersType#KAFKA}
     */
    @Override
    public BrokersType getBrokerType() {
        return BrokersType.KAFKA;
    }

    /**
     * Проверяет работоспособность брокера.
     * В упрощенной реализации проверяет только наличие Producer.
     *
     * @return true если брокер работает корректно, иначе false
     */
    @Override
    public boolean isHealthy() {
        return isConnected();
    }
}
