class MessageBrokersApp {
    constructor() {
        this.baseUrl = '/api';
        this.init();
    }

    init() {
        console.log('Message Brokers Comparison App initialized');
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Global event listeners can be added here
    }

    // Common API call method
    async apiCall(endpoint, options = {}) {
        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, {
                headers: {
                    'Content-Type': 'application/json',
                },
                ...options
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API call failed:', error);
            this.showError('API call failed: ' + error.message);
            throw error;
        }
    }

    // Show success message
    showSuccess(message) {
        this.showAlert(message, 'success');
    }

    // Show error message
    showError(message) {
        this.showAlert(message, 'danger');
    }

    // Show alert
    showAlert(message, type) {
        // Create alert element
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        // Add to page
        const container = document.querySelector('.container') || document.body;
        container.insertBefore(alertDiv, container.firstChild);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.remove();
            }
        }, 5000);
    }

    // Format timestamp
    formatTimestamp(timestamp) {
        if (!timestamp) return 'N/A';

        try {
            const date = new Date(timestamp);
            return date.toLocaleString();
        } catch (error) {
            return timestamp;
        }
    }

    // Format number with commas
    formatNumber(num) {
        return new Intl.NumberFormat().format(num);
    }

    // Get broker status badge HTML
    getStatusBadge(isConnected, isHealthy) {
        if (!isConnected) {
            return '<span class="badge bg-danger"><i class="fas fa-times-circle me-1"></i>Disconnected</span>';
        }

        if (!isHealthy) {
            return '<span class="badge bg-warning"><i class="fas fa-exclamation-triangle me-1"></i>Unhealthy</span>';
        }

        return '<span class="badge bg-success"><i class="fas fa-check-circle me-1"></i>Connected</span>';
    }

    // Функции для управления брокерами
    async startBroker(brokerType) {
        try {
            const response = await this.apiCall(`/messages/${brokerType}/start`, {
                method: 'POST'
            });

            if (response.status === 'SUCCESS') {
                this.addLog(brokerType, `Брокер успешно запущен`, 'success');
                this.updateBrokerStatus(brokerType, 'RUNNING');
            } else {
                this.addLog(brokerType, `Ошибка запуска: ${response.message}`, 'error');
            }
        } catch (error) {
            this.addLog(brokerType, `Ошибка соединения: ${error.message}`, 'error');
        }
    }

    async stopBroker(brokerType) {
        try {
            const response = await this.apiCall(`/messages/${brokerType}/stop`, {
                method: 'POST'
            });

            if (response.status === 'SUCCESS') {
                this.addLog(brokerType, `Брокер остановлен`, 'success');
                this.updateBrokerStatus(brokerType, 'STOPPED');
            } else {
                this.addLog(brokerType, `Ошибка остановки: ${response.message}`, 'error');
            }
        } catch (error) {
            this.addLog(brokerType, `Ошибка соединения: ${error.message}`, 'error');
        }
    }

    async sendMessage(brokerType) {
        const messageInput = document.getElementById(`${brokerType}-message`);
        const message = messageInput.value.trim();

        if (!message) {
            this.showError('Пожалуйста, введите сообщение');
            return;
        }

        try {
            const response = await this.apiCall(`/messages/${brokerType}/send`, {
                method: 'POST',
                body: JSON.stringify({
                    message: message,
                    destination: 'test.queue'
                })
            });

            if (response.status === 'SUCCESS') {
                this.addLog(brokerType, `Сообщение отправлено: ${message}`, 'success');
                messageInput.value = '';
                // Обновляем метрики через 1 секунду
                setTimeout(() => this.updateMetrics(), 1000);
            } else {
                this.addLog(brokerType, `Ошибка отправки: ${response.message}`, 'error');
            }
        } catch (error) {
            this.addLog(brokerType, `Ошибка соединения: ${error.message}`, 'error');
        }
    }

    async updateMetrics() {
        try {
            const data = await this.apiCall('/messages/metrics');

            if (data.status === 'SUCCESS') {
                const metrics = data.metrics;

                // Обновляем метрики для каждого брокера
                ['rabbitmq', 'kafka', 'activemq'].forEach(broker => {
                    const brokerMetrics = metrics[broker];
                    if (brokerMetrics) {
                        // Обновляем UI элементы
                        const sentElement = document.getElementById(`${broker}-sent`);
                        const receivedElement = document.getElementById(`${broker}-received`);
                        const latencyElement = document.getElementById(`${broker}-latency`);
                        const statusElement = document.getElementById(`${broker}-status`);

                        if (sentElement) sentElement.textContent = brokerMetrics.messagesSent || 0;
                        if (receivedElement) receivedElement.textContent = brokerMetrics.messagesReceived || 0;
                        if (latencyElement) latencyElement.textContent = brokerMetrics.averageLatency || 0;
                        if (statusElement) {
                            statusElement.textContent = brokerMetrics.status === 'RUNNING' ? 'ЗАПУЩЕН' : 'ОСТАНОВЛЕН';
                            statusElement.className = `badge ${brokerMetrics.status === 'RUNNING' ? 'status-running' : 'status-stopped'}`;
                        }
                    }
                });
            }
        } catch (error) {
            console.error('Ошибка обновления метрик:', error);
        }
    }

    updateBrokerStatus(brokerType, status) {
        const statusElement = document.getElementById(`${brokerType}-status`);
        if (statusElement) {
            statusElement.textContent = status === 'RUNNING' ? 'ЗАПУЩЕН' : 'ОСТАНОВЛЕН';
            statusElement.className = `badge ${status === 'RUNNING' ? 'status-running' : 'status-stopped'}`;
        }
    }

    addLog(brokerType, message, type = 'info') {
        const logsContainer = document.getElementById(`${brokerType}-logs`);
        if (logsContainer) {
            const logEntry = document.createElement('div');
            logEntry.className = `log-entry log-${type}`;
            logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
            logsContainer.appendChild(logEntry);
            logsContainer.scrollTop = logsContainer.scrollHeight;

            // Ограничиваем количество логов (последние 20 записей)
            if (logsContainer.children.length > 20) {
                logsContainer.removeChild(logsContainer.firstChild);
            }
        }
    }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.app = new MessageBrokersApp();
});

// Utility function to debounce API calls
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Глобальные функции для вызова из HTML
function startBroker(brokerType) {
    if (window.app) {
        window.app.startBroker(brokerType);
    }
}

function stopBroker(brokerType) {
    if (window.app) {
        window.app.stopBroker(brokerType);
    }
}

function sendMessage(brokerType) {
    if (window.app) {
        window.app.sendMessage(brokerType);
    }
}

// Автоматическое обновление метрик каждые 3 секунды
setInterval(() => {
    if (window.app) {
        window.app.updateMetrics();
    }
}, 3000);

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    if (window.app) {
        // Первое обновление метрик через 1 секунду после загрузки
        setTimeout(() => window.app.updateMetrics(), 1000);
    }
});
