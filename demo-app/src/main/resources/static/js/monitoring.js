const monitoring = {
    autoRefreshInterval: null,
    throughputChart: null,
    messageCount: 0,

    init: function() {
        this.initializeCharts();
        this.refreshAllData();
        this.startAutoRefresh();
        this.startMessageSimulation();
    },

    initializeCharts: function() {
        const ctx = document.getElementById('throughputChart').getContext('2d');
        this.throughputChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: Array.from({length: 10}, (_, i) => `${i * 5}с`),
                datasets: [
                    {
                        label: 'ActiveMQ',
                        data: [],
                        borderColor: 'rgb(75, 192, 192)',
                        tension: 0.1,
                        fill: false
                    },
                    {
                        label: 'RabbitMQ',
                        data: [],
                        borderColor: 'rgb(255, 99, 132)',
                        tension: 0.1,
                        fill: false
                    },
                    {
                        label: 'Kafka',
                        data: [],
                        borderColor: 'rgb(54, 162, 235)',
                        tension: 0.1,
                        fill: false
                    },
                    {
                        label: 'WebSocket',
                        data: [],
                        borderColor: 'rgb(255, 205, 86)',
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

    refreshAllData: async function() {
        await this.refreshBrokersStatus();
        await this.refreshSystemMetrics();
        this.updateMessageLog('info', 'Данные обновлены вручную');
    },

    refreshBrokersStatus: async function() {
        try {
            const response = await fetch('/api/messages/status');
            const brokers = await response.json();

            let activeCount = 0;
            let html = '';

            brokers.forEach(broker => {
                const statusClass = broker.connected ? 'broker-active' : 'broker-inactive';
                const statusIcon = broker.connected ? 'fa-check-circle text-success' : 'fa-times-circle text-danger';
                const statusText = broker.connected ? 'Подключен' : 'Отключен';

                if (broker.connected) activeCount++;

                html += `
                    <div class="card status-card ${statusClass} mb-2">
                        <div class="card-body py-2">
                            <div class="row align-items-center">
                                <div class="col-1">
                                    <i class="fas ${statusIcon}"></i>
                                </div>
                                <div class="col-5">
                                    <strong>${broker.brokerType}</strong>
                                </div>
                                <div class="col-3">
                                    <span class="badge ${broker.connected ? 'bg-success' : 'bg-danger'}">
                                        ${statusText}
                                    </span>
                                </div>
                                <div class="col-3 text-end">
                                    <small class="text-muted">${broker.messageCount || 0} сообщ</small>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            });

            document.getElementById('brokersStatus').innerHTML = html;
            document.getElementById('activeBrokersCount').textContent = activeCount;

        } catch (error) {
            console.error('Error fetching brokers status:', error);
            this.updateMessageLog('error', `Ошибка получения статуса брокеров: ${error.message}`);
        }
    },

    refreshSystemMetrics: async function() {
        try {
            // Simulate system metrics
            const memoryUsage = Math.floor(Math.random() * 30) + 10;
            const cpuUsage = Math.floor(Math.random() * 20) + 5;
            const activeThreads = Math.floor(Math.random() * 20) + 5;
            const heapMemory = Math.floor(Math.random() * 500) + 100;
            const systemLoad = Math.floor(Math.random() * 15) + 5;

            document.getElementById('memoryUsage').textContent = memoryUsage + '%';
            document.getElementById('memoryProgress').style.width = memoryUsage + '%';
            document.getElementById('cpuProgress').style.width = cpuUsage + '%';
            document.getElementById('activeThreads').textContent = activeThreads;
            document.getElementById('heapMemory').textContent = heapMemory + ' МБ';
            document.getElementById('systemLoad').textContent = systemLoad + '%';

            // Update total messages (simulated)
            this.messageCount += Math.floor(Math.random() * 5);
            document.getElementById('totalMessages').textContent = this.messageCount;

            // Update uptime
            const uptimeElement = document.getElementById('systemUptime');
            const currentUptime = uptimeElement.textContent;
            const currentSeconds = parseInt(currentUptime) || 0;
            uptimeElement.textContent = (currentSeconds + 5) + 'с';

            // Update throughput chart
            if (this.throughputChart) {
                this.throughputChart.data.datasets.forEach(dataset => {
                    const newData = Math.floor(Math.random() * 100) + 10;
                    dataset.data.push(newData);
                    if (dataset.data.length > 10) {
                        dataset.data.shift();
                    }
                });
                this.throughputChart.update('none');
            }

        } catch (error) {
            console.error('Error refreshing system metrics:', error);
            this.updateMessageLog('error', `Ошибка обновления метрик системы: ${error.message}`);
        }
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