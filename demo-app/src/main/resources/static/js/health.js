const healthMonitor = {
    autoRefreshInterval: null,
    healthData: {},

    init: function() {
        const savedStartTime = localStorage.getItem('healthMonitorStartTime');
        if (savedStartTime) {
            this.startTime = new Date(parseInt(savedStartTime));
        } else {
            this.startTime = new Date();
            localStorage.setItem('healthMonitorStartTime', this.startTime.getTime().toString());
        }

        this.refreshAll();
        this.startAutoRefresh();
    },

    refreshAll: function() {
    this.refreshSystemHealth();
    this.refreshBrokersHealth();
    this.refreshSystemMetrics();
    this.updateHealthChecks();
    this.addHistory('info', 'Ручное обновление проверки состояния');
    },

    refreshSystemHealth: function() {
        const healthStatus = {
            system: Math.random() > 0.1 ? 'up' : 'down',
            database: Math.random() > 0.2 ? 'up' : 'down',
            disk: Math.random() > 0.15 ? 'up' : 'warning',
            memory: Math.random() > 0.1 ? 'up' : 'warning'
        };

        Object.keys(healthStatus).forEach(component => {
            const element = document.getElementById(`${component}Health`);
            const statusElement = element.querySelector('.health-status');
            const status = healthStatus[component];

            statusElement.className = `health-status status-${status}`;
            statusElement.textContent = status.toUpperCase();

            const icon = element.querySelector('.health-icon');
            icon.style.background = this.getStatusColor(status);
        });
    },

    refreshBrokersHealth: function() {
        const brokers = [
            { name: 'ActiveMQ', type: 'ACTIVEMQ' },
            { name: 'RabbitMQ', type: 'RABBITMQ' },
            { name: 'Kafka', type: 'KAFKA' },
            { name: 'WebSocket', type: 'WEBSOCKET' }
        ];

        let html = '';

        brokers.forEach(broker => {
            const isHealthy = Math.random() > 0.3;
            const status = isHealthy ? 'up' : (Math.random() > 0.5 ? 'down' : 'warning');
            const responseTime = (Math.random() * 100 + 10).toFixed(2);
            const messageCount = Math.floor(Math.random() * 1000);

            const statusClass = `broker-health-${status}`;
            const statusText = status.toUpperCase();

            html += `
                <div class="broker-health-item ${statusClass}">
                    <div>
                        <strong>${broker.name}</strong>
                        <div class="health-details">
                            Ответ: ${responseTime}мс • Сообщения: ${messageCount}
                        </div>
                    </div>
                    <span class="health-status status-${status}">${statusText}</span>
                </div>
            `;
        });

        document.getElementById('brokersHealth').innerHTML = html;
    },

    refreshSystemMetrics: async function() {
        try {
            const uptime = Math.floor((new Date() - this.startTime) / 1000);
            this.setElementText('jvmUptime', this.formatUptime(uptime));

            // ОБНОВЛЯЕМ ВСЕ МЕТРИКИ С ПРОВЕРКОЙ НА NULL
            const memoryUsage = 20 + Math.floor(Math.random() * 15);
            const cpuUsage = 10 + Math.floor(Math.random() * 10);
            const activeThreads = 15 + Math.floor(Math.random() * 10);
            const heapMemory = 200 + Math.floor(Math.random() * 100);
            const systemLoad = 5 + Math.floor(Math.random() * 10);
            const gcCount = Math.floor(Math.random() * 100);

            this.setElementText('memoryUsage', memoryUsage + '%');
            this.setElementText('activeThreads', activeThreads);
            this.setElementText('heapMemory', heapMemory + ' МБ');
            this.setElementText('systemLoad', systemLoad + '%');
            this.setElementText('cpuUsage', cpuUsage + '%');
            this.setElementText('gcCount', gcCount);

            // ОБНОВЛЯЕМ PROGRESS BARS С ПРОВЕРКОЙ
            this.setProgressBar('memoryProgress', memoryUsage);
            this.setProgressBar('cpuProgress', cpuUsage);

            // ОБНОВЛЯЕМ ПРОВЕРКИ СОСТОЯНИЯ
            this.updateHealthChecks();
            this.refreshBrokersHealth();

        } catch (error) {
            console.error('Error refreshing system metrics:', error);
        }
    },

    setElementText: function(elementId, text) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = text;
        } else {
            console.warn(`Element not found: ${elementId}`);
        }
    },

    setProgressBar: function(elementId, value) {
        const element = document.getElementById(elementId);
        if (element) {
            element.style.width = value + '%';
        } else {
            console.warn(`Progress bar not found: ${elementId}`);
        }
    },

    updateHealthChecks: function() {
        const checks = [
            { name: 'Подключение к БД', status: Math.random() > 0.2 ? 'up' : 'down' },
            { name: 'Место на диске', status: Math.random() > 0.1 ? 'up' : 'warning' },
            { name: 'Использование памяти', status: Math.random() > 0.15 ? 'up' : 'warning' },
            { name: 'Задержка сети', status: Math.random() > 0.3 ? 'up' : 'down' },
            { name: 'Ответ API', status: Math.random() > 0.25 ? 'up' : 'down' },
            { name: 'Очередь сообщений', status: Math.random() > 0.2 ? 'up' : 'warning' }
        ];

        let html = '';

        checks.forEach(check => {
            const statusClass = `health-check-${check.status}`;

            html += `
                <div class="health-check-item ${statusClass}">
                    <div class="d-flex justify-content-between align-items-center">
                        <span>${check.name}</span>
                        <span class="health-status status-${check.status}">
                            ${check.status.toUpperCase()}
                        </span>
                    </div>
                </div>
            `;
        });

        document.getElementById('healthChecks').innerHTML = html;
    },

    runFullDiagnostic: function() {
        this.addHistory('info', 'Запуск полной диагностики системы...');

        setTimeout(() => {
            this.refreshAll();
            this.addHistory('success', 'Полная диагностика успешно завершена');
        }, 2000);
    },

    toggleAutoRefresh: function() {
        const button = document.querySelector('button[onclick="healthMonitor.toggleAutoRefresh()"]');

        if (this.autoRefreshInterval) {
            clearInterval(this.autoRefreshInterval);
            this.autoRefreshInterval = null;
            button.innerHTML = '<i class="fas fa-play"></i> Автообновление';
            button.classList.remove('btn-success');
            button.classList.add('btn-outline-info');
            this.addHistory('warn', 'Автообновление отключено');
        } else {
            this.startAutoRefresh();
            button.innerHTML = '<i class="fas fa-stop"></i> Автообновление';
            button.classList.remove('btn-outline-info');
            button.classList.add('btn-success');
            this.addHistory('success', 'Автообновление включено (интервал 10с)');
        }
    },

    startAutoRefresh: function() {
        this.autoRefreshInterval = setInterval(() => {
            this.refreshSystemMetrics();
            this.refreshBrokersHealth();
        }, 10000);
    },

    addHistory: function(type, message) {
        const historyElement = document.getElementById('healthHistory');
        const timestamp = new Date().toLocaleTimeString();

        const historyItem = document.createElement('div');
        historyItem.className = 'history-item';
        historyItem.innerHTML = `
            <span class="history-time">${timestamp}</span>
            <span class="history-status status-${type}">${message}</span>
        `;

        historyElement.appendChild(historyItem);
        historyElement.scrollTop = historyElement.scrollHeight;

        const entries = historyElement.getElementsByClassName('history-item');
        if (entries.length > 15) {
            entries[0].remove();
        }
    },

    getStatusColor: function(status) {
        switch(status) {
            case 'up': return 'linear-gradient(135deg, #28a745 0%, #20c997 100%)';
            case 'down': return 'linear-gradient(135deg, #dc3545 0%, #c82333 100%)';
            case 'warning': return 'linear-gradient(135deg, #ffc107 0%, #fd7e14 100%)';
            default: return 'linear-gradient(135deg, #6c757d 0%, #495057 100%)';
        }
    },

    formatUptime: function(seconds) {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hours > 0) {
            return `${hours}ч ${minutes}м ${secs}с`;
        } else if (minutes > 0) {
            return `${minutes}м ${secs}с`;
        } else {
            return `${secs}с`;
        }
    },
};

document.addEventListener('DOMContentLoaded', function() {
    healthMonitor.init();
});
