const monitoring = {
    autoRefreshInterval: null,
    throughputChart: null,
    messageCount: 0,
    startTime: new Date(),

    init: function() {
        this.initializeCharts();
        this.refreshAllData();
        this.startAutoRefresh();
        this.startMessageSimulation();
    },

    initializeCharts: function() {
        const ctx = document.getElementById('throughputChart');
        if (!ctx) {
            console.error('Chart canvas not found');
            return;
        }

        this.throughputChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: Array.from({length: 10}, (_, i) => `${i * 5}с`),
                datasets: [
                    {
                        label: 'ActiveMQ',
                        data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0], // ЯВНО 10 НУЛЕЙ
                        borderColor: 'rgb(75, 192, 192)',
                        tension: 0.1,
                        fill: false
                    },
                    {
                        label: 'RabbitMQ',
                        data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0], // ЯВНО 10 НУЛЕЙ
                        borderColor: 'rgb(255, 99, 132)',
                        tension: 0.1,
                        fill: false
                    },
                    {
                        label: 'Kafka',
                        data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0], // ЯВНО 10 НУЛЕЙ
                        borderColor: 'rgb(54, 162, 235)',
                        tension: 0.1,
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        suggestedMax: 200, // МАКСИМАЛЬНОЕ ЗНАЧЕНИЕ ДЛЯ ШКАЛЫ
                        title: {
                            display: true,
                            text: 'Сообщений в секунду'
                        },
                        ticks: {
                            callback: function(value) {
                                return value + ' msg/s';
                            }
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Время'
                        }
                    }
                }
            }
        });

        console.log('Chart initialized successfully'); // ДЛЯ ДЕБАГА
    },

    refreshAllData: async function() {
        await this.refreshBrokersStatus();
        await this.refreshSystemMetrics();
        this.updateMessageLog('info', 'Данные обновлены вручную');
    },

    refreshBrokersStatus: async function() {
        try {
            const response = await fetch('/api/messages/status');
            const data = await response.json();

            if (data.status === 'SUCCESS') {
                const brokersStatus = data.brokers;
                const brokersHealth = data.health;
                let activeCount = 0;
                let html = '';

                // Проходим по всем типам брокеров
                const brokerTypes = {
                    'ACTIVEMQ': 'ActiveMQ',
                    'RABBITMQ': 'RabbitMQ',
                    'KAFKA': 'Kafka',
                    'WEBSOCKET': 'WebSocket'
                };

                for (const [brokerKey, brokerName] of Object.entries(brokerTypes)) {
                    const isConnected = brokersStatus[brokerKey] || false;
                    const isHealthy = brokersHealth[brokerKey] || false;

                    const statusClass = isConnected ? 'broker-active' : 'broker-inactive';
                    const statusIcon = isConnected ? 'fa-check-circle text-success' : 'fa-times-circle text-danger';
                    const statusText = isConnected ? 'Подключен' : 'Отключен';
                    const healthText = isHealthy ? 'Здоров' : 'Проблемы';

                    if (isConnected) activeCount++;

                    html += `
                        <div class="card status-card ${statusClass} mb-2">
                            <div class="card-body py-2">
                                <div class="row align-items-center">
                                    <div class="col-1">
                                        <i class="fas ${statusIcon}"></i>
                                    </div>
                                    <div class="col-4">
                                        <strong>${brokerName}</strong>
                                    </div>
                                    <div class="col-3">
                                        <span class="badge ${isConnected ? 'bg-success' : 'bg-danger'}">
                                            ${statusText}
                                        </span>
                                    </div>
                                    <div class="col-2">
                                        <small class="${isHealthy ? 'text-success' : 'text-warning'}">
                                            ${healthText}
                                        </small>
                                    </div>
                                    <div class="col-2 text-end">
                                        <small class="text-muted">-</small>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                }

                document.getElementById('brokersStatus').innerHTML = html;
                document.getElementById('activeBrokersCount').textContent = activeCount;

            } else {
                this.updateMessageLog('error', 'Не удалось получить статус брокеров: ' + data.message);
            }
        } catch (error) {
            console.error('Error fetching brokers status:', error);
            this.updateMessageLog('error', `Ошибка получения статуса брокеров: ${error.message}`);

            // Покажем сообщение об ошибке в интерфейсе
            document.getElementById('brokersStatus').innerHTML = `
                <div class="alert alert-warning">
                    <i class="fas fa-exclamation-triangle"></i>
                    Не удалось загрузить статус брокеров. Проверьте подключение.
                </div>
            `;
        }
    },

    refreshSystemMetrics: async function() {
        try {
            // Получаем РЕАЛЬНЫЕ метрики из API вместо случайных
//            const metricsResponse = await fetch('/api/messages/metrics');
//            if (metricsResponse.ok) {
//                const metricsData = await metricsResponse.json();
//
//                if (metricsData.status === 'SUCCESS') {
//                    this.updateRealMetrics(metricsData.metrics);
//                    return;
//                }
//            }

            // Если API не доступно, используем улучшенную симуляцию
            this.updateSimulatedMetrics();

        } catch (error) {
            console.error('Error refreshing system metrics:', error);
            this.updateSimulatedMetrics();
        }
    },

    updateRealMetrics: function(metrics) {
        // Время работы системы (реальное)
        const uptime = Math.floor((new Date() - this.startTime) / 1000);
        const hours = Math.floor(uptime / 3600);
        const minutes = Math.floor((uptime % 3600) / 60);
        const seconds = uptime % 60;
        document.getElementById('systemUptime').textContent =
            `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

        // Обновляем метрики для каждого брокера из реальных данных
        let totalMessages = 0;

        ['activemq', 'rabbitmq', 'kafka'].forEach(broker => {
            const brokerMetrics = metrics[broker];
            if (brokerMetrics) {
                totalMessages += brokerMetrics.messagesSent || 0;

                // ОБНОВЛЯЕМ ГРАФИК С РЕАЛЬНЫМИ ДАННЫМИ
                this.updateThroughputChart(broker, brokerMetrics.messagesPerSecond || 0);
            }
        });

        document.getElementById('totalMessages').textContent = totalMessages;

        // Системные метрики (все еще симулируем, но более реалистично)
        const memoryUsage = 20 + Math.floor(Math.random() * 15);
        const cpuUsage = 10 + Math.floor(Math.random() * 10);

        document.getElementById('memoryUsage').textContent = memoryUsage + '%';
        document.getElementById('memoryProgress').style.width = memoryUsage + '%';
        document.getElementById('cpuProgress').style.width = cpuUsage + '%';
        document.getElementById('activeThreads').textContent = 15 + Math.floor(Math.random() * 10);
        document.getElementById('heapMemory').textContent = (200 + Math.floor(Math.random() * 100)) + ' МБ';
        document.getElementById('systemLoad').textContent = (5 + Math.floor(Math.random() * 10)) + '%';
    },

    updateThroughputChart: function(broker, throughput) {
        if (!this.throughputChart) return;

        const datasetIndex = this.getDatasetIndexForBroker(broker);

        if (datasetIndex >= 0) {
            const dataset = this.throughputChart.data.datasets[datasetIndex];
            dataset.data.push(throughput);

            if (dataset.data.length > 10) {
                dataset.data.shift();
            }

            this.throughputChart.update('none');
        }
    },

    getDatasetIndexForBroker: function(broker) {
        const normalizedBroker = broker.toLowerCase().trim();

        const brokerMap = {
            'activemq': 0,
            'rabbitmq': 1,
            'kafka': 2
        };

        return brokerMap[normalizedBroker] ?? -1;
    },

    updateSimulatedMetrics: function() {
        // Реальное время работы
        const uptime = Math.floor((new Date() - this.startTime) / 1000);
        const hours = Math.floor(uptime / 3600);
        const minutes = Math.floor((uptime % 3600) / 60);
        const seconds = uptime % 60;
        document.getElementById('systemUptime').textContent =
            `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

         const throughputs = {
                'activemq': Math.floor(50 + Math.random() * 30),
                'rabbitmq': Math.floor(70 + Math.random() * 40),
                'kafka': Math.floor(120 + Math.random() * 50)
            };

            // Обновляем график
            Object.keys(throughputs).forEach(broker => {
                console.log(`Calling updateThroughputChart for: ${broker}`);
                this.updateThroughputChart(broker, throughputs[broker]);
            });

        // Остальные метрики (без изменений)
        this.messageCount += Math.floor(Math.random() * 3);
        document.getElementById('totalMessages').textContent = this.messageCount;

        const memoryUsage = 20 + Math.floor(Math.random() * 15);
        const cpuUsage = 10 + Math.floor(Math.random() * 10);

        document.getElementById('memoryUsage').textContent = memoryUsage + '%';
        document.getElementById('memoryProgress').style.width = memoryUsage + '%';
        document.getElementById('cpuProgress').style.width = cpuUsage + '%';
        document.getElementById('activeThreads').textContent = 15 + Math.floor(Math.random() * 10);
        document.getElementById('heapMemory').textContent = (200 + Math.floor(Math.random() * 100)) + ' МБ';
        document.getElementById('systemLoad').textContent = (5 + Math.floor(Math.random() * 10)) + '%';
    },

    updateMessageLog: function(type, message) {
        const logElement = document.getElementById('messageLog');
        const timestamp = new Date().toLocaleTimeString();
        const logClass = `log-${type}`;

        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${logClass}`;
        logEntry.innerHTML = `<small>[${timestamp}]</small> ${message}`;

        logElement.appendChild(logEntry);
        logElement.scrollTop = logElement.scrollHeight;

        // Keep only last 50 entries
        const entries = logElement.getElementsByClassName('log-entry');
        if (entries.length > 50) {
            entries[0].remove();
        }
    },

    clearMessageLog: function() {
        document.getElementById('messageLog').innerHTML =
            '<div class="log-entry log-info">Лог очищен. Мониторинг продолжается...</div>';
    },

    toggleAutoRefresh: function() {
        const button = document.querySelector('button[onclick="monitoring.toggleAutoRefresh()"]');

        if (this.autoRefreshInterval) {
            clearInterval(this.autoRefreshInterval);
            this.autoRefreshInterval = null;
            button.innerHTML = '<i class="fas fa-play"></i> Автообновление';
            button.classList.remove('btn-success');
            button.classList.add('btn-outline-info');
            this.updateMessageLog('warn', 'Автообновление отключено');
        } else {
            this.startAutoRefresh();
            button.innerHTML = '<i class="fas fa-stop"></i> Автообновление';
            button.classList.remove('btn-outline-info');
            button.classList.add('btn-success');
            this.updateMessageLog('success', 'Автообновление включено (интервал 5с)');
        }
    },

    startAutoRefresh: function() {
        this.autoRefreshInterval = setInterval(() => {
            this.refreshBrokersStatus();
            this.refreshSystemMetrics();
        }, 5000);
    },

    startMessageSimulation: function() {
        setInterval(() => {
            const brokers = ['ActiveMQ', 'RabbitMQ', 'Kafka', 'WebSocket'];
            const actions = ['отправлено', 'получено', 'обработано', 'подтверждено'];
            const types = ['info', 'success'];

            if (Math.random() > 0.3) {
                const broker = brokers[Math.floor(Math.random() * brokers.length)];
                const action = actions[Math.floor(Math.random() * actions.length)];
                const type = types[Math.floor(Math.random() * types.length)];

                this.updateMessageLog(type, `${broker} сообщение ${action}`);
            }
        }, 2000);
    }
};

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    monitoring.init();
});
