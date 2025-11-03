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
        messageHistory: []
    },

    init: function() {
        this.initializeCharts();
        this.setupEventListeners();
        this.updateUI();
    },

    initializeCharts: function() {
        // Main Chart
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

        // Frequency Chart
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

        // Throughput Chart
        const throughputCtx = document.getElementById('throughputChart').getContext('2d');
        this.charts.throughput = new Chart(throughputCtx, {
            type: 'line',
            data: {
                labels: Array.from({length: 10}, (_, i) => `${i}с`),
                datasets: [{
                    label: 'Сообщений/сек',
                    data: [],
                    borderColor: 'rgb(54, 162, 235)',
                    tension: 0.1
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
    },

    setupEventListeners: function() {
        // Range inputs
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

        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            this.connected = true;
            this.data.connectionStart = new Date();
            this.updateConnectionStatus('connected', 'Подключено');
            this.updateUI();
            this.addLog('success', 'WebSocket успешно подключен');

            // Subscribe to topics
            this.stompClient.subscribe('/topic/livedata', (message) => {
                this.handleLiveData(JSON.parse(message.body));
            });

            this.stompClient.subscribe('/topic/statistics', (message) => {
                this.handleStatistics(JSON.parse(message.body));
            });

        }, (error) => {
            this.connected = false;
            this.updateConnectionStatus('disconnected', 'Ошибка подключения');
            this.updateUI();
            this.addLog('error', `Ошибка подключения: ${error}`);
        });
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

        // Update main chart
        const timestamp = new Date().toLocaleTimeString();
        this.updateMainChart(timestamp, data.value);

        // Update statistics
        this.updateStatistics();

        // Update frequency analysis (simulated)
        this.updateFrequencyAnalysis(data.value);

        // Add to message log
        this.addLog('info', `Данные: ${data.value.toFixed(2)} (${data.type})`);
    },

    handleStatistics: function(stats) {
        // Handle statistics data from server
        this.addLog('info', `Обновление статистики: ${JSON.stringify(stats)}`);
    },

    updateMainChart: function(label, value) {
        const chart = this.charts.main;

        // Add new data point
        chart.data.labels.push(label);
        chart.data.datasets[0].data.push(value);

        // Keep only last 20 points
        if (chart.data.labels.length > 20) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }

        chart.update('none');
    },

    updateFrequencyAnalysis: function(value) {
        // Simulate frequency analysis based on current value
        const frequencies = Array.from({length: 5}, () => Math.random() * 100);
        this.charts.frequency.data.datasets[0].data = frequencies;
        this.charts.frequency.update('none');
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

    startGenerator: function() {
        if (this.generatorInterval) return;

        const interval = parseInt(document.getElementById('updateInterval').value);
        const amplitude = parseInt(document.getElementById('amplitude').value);
        const frequency = parseInt(document.getElementById('frequency').value);
        const dataType = document.getElementById('dataType').value;

        let time = 0;

        this.generatorInterval = setInterval(() => {
            if (!this.connected) return;

            let value;
            switch(dataType) {
                case 'sine':
                    value = amplitude * Math.sin(2 * Math.PI * frequency * time / 1000);
                    break;
                case 'cosine':
                    value = amplitude * Math.cos(2 * Math.PI * frequency * time / 1000);
                    break;
                case 'random':
                    value = (Math.random() - 0.5) * 2 * amplitude;
                    break;
                case 'sawtooth':
                    value = 2 * amplitude * (time / 1000 * frequency % 1) - amplitude;
                    break;
                default:
                    value = 0;
            }

            // Send simulated data (in real app, this would come from server)
            const data = {
                type: dataType,
                value: value,
                timestamp: new Date().toISOString(),
                amplitude: amplitude,
                frequency: frequency
            };

            this.handleLiveData(data);
            time += interval;

        }, interval);

        document.getElementById('startGeneratorBtn').disabled = true;
        document.getElementById('stopGeneratorBtn').disabled = false;
        this.addLog('success', `Генератор данных запущен (${dataType}, ${interval}мс)`);
    },

    stopGenerator: function() {
        if (this.generatorInterval) {
            clearInterval(this.generatorInterval);
            this.generatorInterval = null;
        }

        document.getElementById('startGeneratorBtn').disabled = false;
        document.getElementById('stopGeneratorBtn').disabled = true;
        this.addLog('info', 'Генератор данных остановлен');
    },

    addLog: function(type, message) {
        const logElement = document.getElementById('messageLog');
        const timestamp = new Date().toLocaleTimeString();

        const logEntry = document.createElement('div');
        logEntry.className = `log-entry log-${type}`;
        logEntry.innerHTML = `<small>[${timestamp}]</small> ${message}`;

        logElement.appendChild(logEntry);
        logElement.scrollTop = logElement.scrollHeight;

        // Keep only last 20 entries
        const entries = logElement.getElementsByClassName('log-entry');
        if (entries.length > 20) {
            entries[0].remove();
        }
    },

    clearData: function() {
        // Clear all charts
        Object.values(this.charts).forEach(chart => {
            chart.data.labels = [];
            chart.data.datasets.forEach(dataset => {
                dataset.data = [];
            });
            chart.update('none');
        });

        // Clear statistics
        this.data.messagesReceived = 0;
        this.data.dataPoints = 0;
        this.updateStatistics();

        // Clear message log
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

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    websocketDemo.init();
});