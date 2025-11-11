package com.vyanckus.websocket.controller;

import com.vyanckus.dto.ChartData;
import com.vyanckus.dto.ChartRequest;
import com.vyanckus.websocket.dto.WebSocketControlMessage;
import com.vyanckus.websocket.service.LiveDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket контроллер для управления демо с графиками в реальном времени.
 * Обрабатывает команды от клиента и предоставляет данные для визуализации.
 *
 * <p><b>Обрабатываемые endpoints:</b>
 * <ul>
 *   <li>/app/websocket/control - управление генератором данных</li>
 *   <li>/app/websocket/chart - запрос данных для графиков</li>
 * </ul>
 */
@Controller
public class WebSocketDemoController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDemoController.class);

    @Autowired
    private LiveDataGenerator liveDataGenerator;

    /**
     * Обрабатывает команды управления WebSocket генератором от клиента.
     * Поддерживает команды start и stop с параметрами генерации.
     *
     * @param message сообщение с командой и параметрами
     */
    @MessageMapping("/websocket/control")
    public void handleControlMessage(WebSocketControlMessage message) {
        log.info("Received WebSocket control command: {} with parameters: {}",
                message.command(), message.parameters());

        switch (message.command()) {
            case "start":
                liveDataGenerator.startGenerator(message.parameters());
                break;
            case "stop":
                liveDataGenerator.stopGenerator();
                break;
            default:
                log.warn("Unknown WebSocket command: {}", message.command());
        }
    }

    /**
     * Обрабатывает запросы на получение данных для графиков.
     * Возвращает структурированные данные в формате ChartData.
     *
     * @param request запрос с параметрами графика
     * @return данные для отображения на графике
     */
    @MessageMapping("/websocket/chart")
    @SendTo("/topic/chartdata")
    public ChartData getChartData(ChartRequest request) {
        log.debug("Received chart data request: {}", request);

        // Генерируем тестовые данные (в реальном проекте брали бы из метрик)
        List<String> labels = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        List<Number> values = Arrays.asList(10, 20, 15, 25, 30, 35, 40, 45, 50, 55);

        String title = "WebSocket Demo";
        if (request.brokerType() != null) {
            title += " - " + request.brokerType().getDisplayName();
        }

        return new ChartData(labels, values, request.chartType(), title);
    }

    /**
     * Проверяет статус генератора данных.
     *
     * @return статус генератора
     */
    @MessageMapping("/websocket/status")
    @SendTo("/topic/generator-status")
    public String getGeneratorStatus() {
        return liveDataGenerator.isGeneratorRunning() ? "RUNNING" : "STOPPED";
    }
}
