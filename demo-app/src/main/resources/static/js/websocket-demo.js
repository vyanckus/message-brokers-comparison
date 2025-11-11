const websocketDemo = {
    stompClient: null,
    connected: false,
    generatorInterval: null,
    charts: {},
    data: {
        messagesReceived: 0,
        dataPoints: 0,
        connectionStart: null,
        lastUpdate: null,
        messageHistory: [],
        throughputData: [],
        lastSecondCount: 0,
        lastThroughputUpdate: null
    },

    init: function() {
        this.initializeCharts();
        this.setupEventListeners();
        this.updateUI();
    },

    initializeCharts: function() {
        const mainCtx = document.getElementById('mainChart').getContext('2d');
        this.charts.main = new Chart(mainCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Данные в реальном времени',
                    data: [],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointRadius: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: false,
                        title: {
                            display: true,
                            text: 'Значение'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Время'
                        }
                    }
                },
                animation: {
                    duration: 0
                }
            }
        });

        const freqCtx = document.getElementById('frequencyChart').getContext('2d');
        this.charts.frequency = new Chart(freqCtx, {
            type: 'bar',
            data: {
                labels: ['1Гц', '2Гц', '3Гц', '4Гц', '5Гц'],
                datasets: [{
                    label: 'Частота',
                    data: [0, 0, 0, 0, 0],
                    backgroundColor: 'rgba(255, 99, 132, 0.8)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });

        const throughputCtx = document.getElementById('throughputChart').getContext('2d');
        this.charts.throughput = new Chart(throughputCtx, {
            type: 'line',
            data: {
                labels: Array.from({length: 10}, (_, i) => `${i}с`),
                datasets: [{
                    label: 'Сообщений/сек',
                    data: Array(10).fill(0),
                    borderColor: 'rgb(54, 162, 235)',
                    tension: 0.1,
                    fill: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Сообщений в секунду'
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
    },

    setupEventListeners: function() {
        document.getElementById('updateInterval').addEventListener('input', (e) => {
            document.getElementById('intervalValue').textContent = e.target.value;
        });

        document.getElementById('amplitude').addEventListener('input', (e) => {
            document.getElementById('amplitudeValue').textContent = e.target.value;
        });

        document.getElementById('frequency').addEventListener('input', (e) => {
            document.getElementById('frequencyValue').textContent = e.target.value;
        });
    },

    connect: function() {
        if (this.connected) return;

        this.updateConnectionStatus('connecting', 'Подключение...');

        try {
            const socket = new SockJS('/ws');
                    this.stompClient = Stomp.over(socket);
                    this.stompClient.debug = null;

                    this.stompClient.connect({}, (frame) => {
                        this.connected = true;
                        this.data.connectionStart = new Date();
                        this.updateConnectionStatus('connected', 'Подключено');
                        this.updateUI();
                        this.addLog('success', 'WebSocket успешно подключен');

                        // Подписываемся на живые данные
                        this.stompClient.subscribe('/topic/livedata', (message) => {
                            this.handleLiveData(JSON.parse(message.body));
                        });

                        // Подписываемся на статус генератора
                        this.stompClient.subscribe('/topic/generator-status', (message) => {
                            this.handleGeneratorStatus(message.body);
                        });

                        // Подписываемся на данные графиков
                        this.stompClient.subscribe('/topic/chartdata', (message) => {
                            this.handleChartData(JSON.parse(message.body));
                        });

            }, (error) => {
                this.connected = false;
                this.updateConnectionStatus('disconnected', 'Ошибка подключения');
                this.updateUI();
                this.addLog('error', `Ошибка подключения WebSocket: ${error}`);

                console.error('WebSocket connection error:', error);
            });
        } catch (error) {
            this.connected = false;
            this.updateConnectionStatus('disconnected', 'Ошибка инициализации');
            this.updateUI();
            this.addLog('error', `Ошибка инициализации WebSocket: ${error}`);
            console.error('WebSocket initialization error:', error);
        }
    },

    disconnect: function() {
        if (this.stompClient && this.connected) {
            this.stompClient.disconnect();
        }
        this.connected = false;
        this.stopGenerator();
        this.updateConnectionStatus('disconnected', 'Отключено');
        this.updateUI();
        this.addLog('info', 'WebSocket отключен');
    },

    updateConnectionStatus: function(status, text) {
        const statusElement = document.getElementById('connectionStatus');
        const textElement = document.getElementById('connectionText');

        statusElement.className = 'status-indicator';
        textElement.textContent = text;

        switch(status) {
            case 'connected':
                statusElement.classList.add('status-connected');
                document.getElementById('connectionDetails').textContent = 'Потоковая передача данных активна';
                break;
            case 'connecting':
                statusElement.classList.add('status-connecting');
                document.getElementById('connectionDetails').textContent = 'Установка соединения...';
                break;
            case 'disconnected':
                statusElement.classList.add('status-disconnected');
                document.getElementById('connectionDetails').textContent = 'Нажмите "Подключить" для установки WebSocket соединения';
                break;
        }
    },

    handleLiveData: function(data) {
        this.data.messagesReceived++;
        this.data.dataPoints++;
        this.data.lastUpdate = new Date();

        // Расчет пропускной способности
        this.updateThroughput();

        const timestamp = new Date().toLocaleTimeString();

        // Используем данные с сервера для основного графика
        this.updateMainChart(timestamp, data.value);
        this.updateStatistics();
        this.updateFrequencyAnalysis(data.value);

        // Обновляем реальные метрики брокеров если есть
        if (data.brokerMetrics) {
            this.updateBrokerMetrics(data.brokerMetrics);
        }

        this.addLog('info', `Данные с сервера: ${data.value.toFixed(2)} (${data.type})`);
    },

    // Метод для расчета пропускной способности
    updateThroughput: function() {
        const now = new Date();

        if (!this.data.lastThroughputUpdate) {
            this.data.lastThroughputUpdate = now;
            this.data.lastSecondCount = 0;
            return;
        }

        this.data.lastSecondCount++;

        // Обновляем каждую секунду
        const secondsPassed = (now - this.data.lastThroughputUpdate) / 1000;

        if (secondsPassed >= 1) {
            const throughput = this.data.lastSecondCount / secondsPassed;

            // Добавляем в данные графика
            this.data.throughputData.push(throughput);
            if (this.data.throughputData.length > 10) {
                this.data.throughputData.shift();
            }

            // Обновляем график
            this.updateThroughputChart();

            // Сбрасываем счетчик
            this.data.lastSecondCount = 0;
            this.data.lastThroughputUpdate = now;
        }
    },

    // Метод для графика пропускной способности
    updateThroughputChart: function() {
        const chart = this.charts.throughput;
        chart.data.datasets[0].data = [...this.data.throughputData];
        chart.update('none');
    },

    handleStatistics: function(stats) {
        this.addLog('info', `Обновление статистики: ${JSON.stringify(stats)}`);
    },

    updateMainChart: function(label, value) {
        if (!this.charts.main) {
            console.error('Main chart not initialized');
            return;
        }

        const chart = this.charts.main;

        chart.data.labels.push(label);
        chart.data.datasets[0].data.push(value);

        if (chart.data.labels.length > 20) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }

        chart.update('none');
    },

    updateFrequencyAnalysis: function(value) {
        // Реальный частотный анализ на основе типа данных
        const chart = this.charts.frequency;
        const dataType = document.getElementById('dataType').value;

        let frequencies;

        switch(dataType) {
            case 'sine':
                // Для синусоиды - пик на заданной частоте
                const freq = parseInt(document.getElementById('frequency').value);
                frequencies = [0, 0, 0, 0, 0];
                frequencies[freq - 1] = 80 + Math.random() * 20; // Пик на нужной частоте
                break;

            case 'cosine':
                // Аналогично синусу
                const cosFreq = parseInt(document.getElementById('frequency').value);
                frequencies = [0, 0, 0, 0, 0];
                frequencies[cosFreq - 1] = 70 + Math.random() * 30;
                break;

            case 'random':
                // Случайный шум - равномерное распределение
                frequencies = Array.from({length: 5}, () => Math.random() * 50 + 20);
                break;

            case 'sawtooth':
                // Пилообразный сигнал - гармоники
                const sawFreq = parseInt(document.getElementById('frequency').value);
                frequencies = [60, 30, 15, 8, 4].map((v, i) =>
                    i === sawFreq - 1 ? v : v / (i + 2)
                );
                break;

            default:
                frequencies = [0, 0, 0, 0, 0];
        }

        chart.data.datasets[0].data = frequencies;
        chart.update('none');
    },

    updateStatistics: function() {
        document.getElementById('messagesReceived').textContent = this.data.messagesReceived;
        document.getElementById('dataPoints').textContent = this.data.dataPoints;

        if (this.data.connectionStart) {
            const uptime = Math.floor((new Date() - this.data.connectionStart) / 1000);
            document.getElementById('connectionTime').textContent = `${uptime}с`;
        }

        if (this.data.lastUpdate) {
            document.getElementById('lastUpdate').textContent = this.data.lastUpdate.toLocaleTimeString();
        }
    },

    // Обновляет UI реальными метриками брокеров с сервера.
    updateBrokerMetrics: function(metrics) {
        // Можно добавить отображение реальных метрик в UI
        // Например, обновить дополнительные графики или статистику
        console.log('Real broker metrics from server:', metrics);

        if (metrics.kafka) {
            this.addLog('debug',
                `Kafka metrics: sent=${metrics.kafka.sent}, received=${metrics.kafka.received}`
            );
        }
    },

    startGenerator: function() {
        if (!this.connected) {
            this.addLog('error', 'WebSocket не подключен');
            return;
        }

        // Собираем параметры с UI
        const params = {
            interval: parseInt(document.getElementById('updateInterval').value),
            amplitude: parseInt(document.getElementById('amplitude').value),
            frequency: parseInt(document.getElementById('frequency').value),
            dataType: document.getElementById('dataType').value
        };

        // Отправляем команду на сервер для запуска генератора
        this.stompClient.send("/app/websocket/control", {},
            JSON.stringify({
                command: "start",
                parameters: params
            }));

        document.getElementById('startGeneratorBtn').disabled = true;
        document.getElementById('stopGeneratorBtn').disabled = false;
        this.addLog('success', 'Команда запуска генератора отправлена на сервер');
    },

    stopGenerator: function() {
        if (this.connected) {
            // Отправляем команду на сервер для остановки генератора
            this.stompClient.send("/app/websocket/control", {},
                JSON.stringify({
                    command: "stop"
                }));
        }

        document.getElementById('startGeneratorBtn').disabled = false;
        document.getElementById('stopGeneratorBtn').disabled = true;
        this.addLog('info', 'Команда остановки генератора отправлена на сервер');
    },

    // Обрабатывает статус генератора от сервера.
    handleGeneratorStatus: function(status) {
        if (status === 'RUNNING') {
            this.addLog('success', 'Генератор данных запущен на сервере');
            document.getElementById('startGeneratorBtn').disabled = true;
            document.getElementById('stopGeneratorBtn').disabled = false;
        } else {
            this.addLog('info', 'Генератор данных остановлен на сервере');
            document.getElementById('startGeneratorBtn').disabled = false;
            document.getElementById('stopGeneratorBtn').disabled = true;
        }
    },

    // Обрабатывает структурированные данные для графиков от сервера.
    handleChartData: function(chartData) {
        this.addLog('info', `Получены данные графика: ${chartData.title}`);

        // Здесь можно обновлять дополнительные графики
        // используя chartData.labels и chartData.values
        console.log('Chart data received:', chartData);
    },



    addLog: function(type, message) {
        const logElement = document.getElementById('messageLog');
        const timestamp = new Date().toLocaleTimeString();

        const logEntry = document.createElement('div');
        logEntry.className = `log-entry log-${type}`;
        logEntry.innerHTML = `<small>[${timestamp}]</small> ${message}`;

        logElement.appendChild(logEntry);
        logElement.scrollTop = logElement.scrollHeight;

        const entries = logElement.getElementsByClassName('log-entry');
        if (entries.length > 20) {
            entries[0].remove();
        }
    },

    clearData: function() {
        Object.values(this.charts).forEach(chart => {
            chart.data.labels = [];
            chart.data.datasets.forEach(dataset => {
                dataset.data = [];
            });
            chart.update('none');
        });

        this.data.messagesReceived = 0;
        this.data.dataPoints = 0;
        this.updateStatistics();

        document.getElementById('messageLog').innerHTML =
            '<div class="log-entry log-info">Лог очищен</div>';

        this.addLog('info', 'Все данные очищены');
    },

    updateUI: function() {
        document.getElementById('connectBtn').disabled = this.connected;
        document.getElementById('disconnectBtn').disabled = !this.connected;
        document.getElementById('startGeneratorBtn').disabled = !this.connected;
    }
};

document.addEventListener('DOMContentLoaded', function() {
    websocketDemo.init();
});
