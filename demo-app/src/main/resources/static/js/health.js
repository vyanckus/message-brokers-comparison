const healthMonitor = {
    autoRefreshInterval: null,
    healthData: {},

    init: function() {
        this.refreshAll();
        this.startAutoRefresh();
    },

    refreshAll: function() {
        this.refreshSystemHealth();
        this.refreshBrokersHealth();
        this.refreshSystemMetrics();
        this.addHistory('info', 'Ручное обновление проверки состояния');
    },

    refreshSystemHealth: function() {
        // Simulate system health checks
        const healthStatus = {
            system: Math.random() > 0.1 ? 'up' : 'down',
            database: Math.random() > 0.2 ? 'up' : 'down',
            disk: Math.random() > 0.15 ? 'up' : 'warning',
            memory: Math.random() > 0.1 ? 'up' : 'warning'
        };

        // Update health cards
        Object.keys(healthStatus).forEach(component => {
            const element = document.getElementById(`${component}Health`);
            const statusElement = element.querySelector('.health-status');
            const status = healthStatus[component];

            statusElement.className = `health-status status-${status}`;
            statusElement.textContent = status.toUpperCase();

            // Update icon color based on status
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
            // Simulate broker health (in real app, this would come from API)
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

    refreshSystemMetrics: function() {
        // Simulate system metrics
        const metrics = {
            jvmUptime: this.formatUptime(Math.floor(Math.random() * 86400) + 3600), // 1-24 hours
            heapMemory: `${(Math.random() * 500 + 100).toFixed(0)} МБ / ${(Math.random() * 1000 + 500).toFixed(0)} МБ`,
            activeThreads: Math.floor(Math.random() * 50 + 10),
            cpuUsage: `${(Math.random() * 30 + 5).toFixed(1)}%`,
            systemLoad: `${(Math.random() * 2 + 0.5).toFixed(2)}`,
            gcCount: Math.floor(Math.random() * 100)
        };

        // Update metrics display
        Object.keys(metrics).forEach(metric => {
            const element = document.getElementById(metric);
            if (element) {
                element.textContent = metrics[metric];
            }
        });

        // Update health checks
        this.updateHealthChecks();
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

        // Simulate diagnostic process
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
        }, 10000); // Refresh every 10 seconds
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

        // Keep only last 15 entries
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
        return `${hours}ч ${minutes}м ${secs}с`;
    }
};

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    healthMonitor.init();
});