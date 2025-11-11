package com.vyanckus.controller;

import com.vyanckus.dto.BrokersType;
import com.vyanckus.service.BenchmarkService;
import com.vyanckus.service.MessageBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Web контроллер для HTML интерфейса приложения.
 *
 * <p>Предоставляет HTML страницы для:</p>
 * <ul>
 *   <li>Главной страницы с обзором брокеров</li>
 *   <li>Страницы тестирования сообщений</li>
 *   <li>Страницы benchmark тестирования</li>
 *   <li>Страницы мониторинга и статистики</li>
 *   <li>WebSocket демо с графиками</li>
 * </ul>
 */
@Controller
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    private final MessageBrokerService messageBrokerService;
    private final BenchmarkService benchmarkService;

    /**
     * Конструктор web контроллера приложения.
     *
     * @param messageBrokerService сервис для работы с брокерами сообщений
     * @param benchmarkService сервис для выполнения benchmark тестов
     */
    public WebController(MessageBrokerService messageBrokerService, BenchmarkService benchmarkService) {
        this.messageBrokerService = messageBrokerService;
        this.benchmarkService = benchmarkService;
        log.info("WebController initialized");
    }

    /**
     * Главная страница приложения.
     *
     * <p>Отображает обзорную информацию о всех брокерах, их статус и основные метрики.</p>
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона главной страницы
     */
    @GetMapping("/")
    public String home(Model model) {
        log.debug("Loading home page");

        // Статус брокеров
        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();
        Map<BrokersType, Boolean> brokersHealth = messageBrokerService.getBrokersHealth();

        // Список всех типов брокеров
        List<BrokersType> allBrokers = Arrays.asList(BrokersType.values());

        model.addAttribute("pageTitle", "Message Brokers Comparison");
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("brokersHealth", brokersHealth);
        model.addAttribute("allBrokers", allBrokers);
        model.addAttribute("initialized", messageBrokerService.isInitialized());
        model.addAttribute("currentTime", java.time.LocalDateTime.now());

        return "index";
    }

    /**
     * Страница тестирования сообщений.
     *
     * <p>Предоставляет интерфейс для отправки и получения сообщений через разные брокеры.</p>
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы тестирования
     */
    @GetMapping("/messages")
    public String messagesPage(Model model) {
        log.debug("Loading messages testing page");

        List<BrokersType> allBrokers = Arrays.asList(BrokersType.values());
        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();

        model.addAttribute("pageTitle", "Message Testing");
        model.addAttribute("allBrokers", allBrokers);
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("defaultDestination", "test.queue");
        model.addAttribute("initialized", messageBrokerService.isInitialized());

        return "messages";
    }

    /**
     * Страница benchmark тестирования.
     *
     * <p>Предоставляет интерфейс для запуска производительностных тестов и сравнения брокеров.</p>
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы benchmark
     */
    @GetMapping("/benchmark")
    public String benchmarkPage(Model model) {
        log.debug("Loading benchmark testing page");

        List<BrokersType> allBrokers = Arrays.asList(BrokersType.values());
        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();
        Map<String, Boolean> activeBenchmarks = benchmarkService.getActiveBenchmarksStatus();

        // Настройки benchmark по умолчанию
        model.addAttribute("pageTitle", "Performance Benchmark");
        model.addAttribute("allBrokers", allBrokers);
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("activeBenchmarks", activeBenchmarks);
        model.addAttribute("defaultMessageCount", 100);
        model.addAttribute("defaultDestination", "benchmark-queue");
        model.addAttribute("initialized", messageBrokerService.isInitialized());

        return "benchmark";
    }

    /**
     * Страница мониторинга и статистики.
     *
     * <p>Отображает детальную статистику работы брокеров, историю сообщений и метрики.</p>
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы мониторинга
     */
    @GetMapping("/monitoring")
    public String monitoringPage(Model model) {
        log.debug("Loading monitoring page");

        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();
        Map<BrokersType, Boolean> brokersHealth = messageBrokerService.getBrokersHealth();
        Map<String, Boolean> activeBenchmarks = benchmarkService.getActiveBenchmarksStatus();

        // Рассчитываем общую статистику
        long connectedBrokers = brokersStatus.values().stream().filter(Boolean::booleanValue).count();
        long healthyBrokers = brokersHealth.values().stream().filter(Boolean::booleanValue).count();

        model.addAttribute("pageTitle", "Monitoring & Statistics");
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("brokersHealth", brokersHealth);
        model.addAttribute("activeBenchmarks", activeBenchmarks);
        model.addAttribute("connectedBrokers", connectedBrokers);
        model.addAttribute("healthyBrokers", healthyBrokers);
        model.addAttribute("totalBrokers", brokersStatus.size());
        model.addAttribute("initialized", messageBrokerService.isInitialized());

        return "monitoring";
    }

    /**
     * Страница WebSocket демонстрации с графиками.
     *
     * <p>Предоставляет реальное время обновление данных через WebSocket и визуализацию в виде графиков.</p>
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы WebSocket демо
     */
    @GetMapping("/websocket-demo")
    public String websocketDemoPage(Model model) {
        log.debug("Loading WebSocket demo page");

        List<BrokersType> allBrokers = Arrays.asList(BrokersType.values());
        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();

        // Данные для демо графиков (можно заменить на реальные данные)
        List<String> chartLabels = List.of("ActiveMQ", "RabbitMQ", "Kafka", "WebSocket");
        List<Number> chartData = List.of(85, 92, 78, 88);

        model.addAttribute("pageTitle", "WebSocket Demo & Charts");
        model.addAttribute("allBrokers", allBrokers);
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("initialized", messageBrokerService.isInitialized());

        return "websocket-demo";
    }

    /**
     * Страница с информацией о проекте.
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы информации
     */
    @GetMapping("/about")
    public String aboutPage(Model model) {
        log.debug("Loading about page");

        List<Map<String, Object>> brokersInfo = Arrays.stream(BrokersType.values())
                .map(broker -> {
                    Map<String, Object> brokerInfo = new java.util.HashMap<>();
                    brokerInfo.put("type", broker);
                    brokerInfo.put("displayName", broker.getDisplayName());
                    brokerInfo.put("description", getBrokerDescription(broker));
                    return brokerInfo;
                })
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("pageTitle", "About Project");
        model.addAttribute("brokersInfo", brokersInfo);
        model.addAttribute("projectVersion", "1.0.0");
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("springBootVersion", "3.0.0");

        return "about";
    }

    /**
     * Страница здоровья приложения (health check).
     *
     * @param model модель для передачи данных в шаблон
     * @return имя шаблона страницы здоровья
     */
    @GetMapping("/health")
    public String healthPage(Model model) {
        log.debug("Loading health page");

        Map<BrokersType, Boolean> brokersStatus = messageBrokerService.getBrokersStatus();
        Map<BrokersType, Boolean> brokersHealth = messageBrokerService.getBrokersHealth();
        Map<String, Boolean> activeBenchmarks = benchmarkService.getActiveBenchmarksStatus();

        // Общий статус приложения
        boolean allBrokersHealthy = brokersHealth.values().stream().allMatch(Boolean::booleanValue);
        boolean noActiveBenchmarks = activeBenchmarks.isEmpty();
        boolean appHealthy = allBrokersHealthy && noActiveBenchmarks && messageBrokerService.isInitialized();

        model.addAttribute("pageTitle", "Application Health");
        model.addAttribute("brokersStatus", brokersStatus);
        model.addAttribute("brokersHealth", brokersHealth);
        model.addAttribute("activeBenchmarks", activeBenchmarks);
        model.addAttribute("appHealthy", appHealthy);
        model.addAttribute("allBrokersHealthy", allBrokersHealthy);
        model.addAttribute("noActiveBenchmarks", noActiveBenchmarks);
        model.addAttribute("initialized", messageBrokerService.isInitialized());
        model.addAttribute("currentTime", java.time.Instant.now());

        return "health";
    }

    /**
     * Вспомогательный метод для получения описания брокера.
     *
     * @param brokerType тип брокера
     * @return описание брокера
     */
    public String getBrokerDescription(BrokersType brokerType) {
        return switch (brokerType) {
            case ACTIVEMQ -> "Apache ActiveMQ - powerful open source message broker with JMS support";
            case RABBITMQ -> "RabbitMQ - most widely deployed open source message broker with AMQP protocol";
            case KAFKA -> "Apache Kafka - distributed event streaming platform for high-performance data pipelines";
            case WEBSOCKET -> "WebSocket - protocol for real-time bidirectional communication between client and server";
        };
    }
}
