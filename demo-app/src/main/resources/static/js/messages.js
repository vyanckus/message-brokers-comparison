class MessagesPage {
    constructor() {
        this.messageHistory = [];
        this.activeSubscriptions = new Set();
        this.init();
    }

    init() {
        console.log('Messages page initialized');
        this.setupEventListeners();
        this.loadMessageHistory();
        this.updateHistoryDisplay();
    }

    setupEventListeners() {
        const sendForm = document.getElementById('sendMessageForm');
        if (sendForm) {
            sendForm.addEventListener('submit', (e) => this.handleSendMessage(e));
        }

        setInterval(() => this.loadMessageHistory(), 10000);
    }

    async handleSendMessage(event) {
        event.preventDefault();

        const brokerType = document.getElementById('brokerType').value;
        const destination = document.getElementById('destination').value;
        const messageContent = document.getElementById('messageContent').value;

        if (!brokerType || !destination || !messageContent) {
            window.app.showError('Пожалуйста, заполните все поля');
            return;
        }

        const sendBtn = document.getElementById('sendMessageBtn');
        const originalText = sendBtn.innerHTML;

        try {
            sendBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Отправка...';
            sendBtn.disabled = true;

            const response = await window.app.apiCall('/messages/send', {
                method: 'POST',
                body: JSON.stringify({
                    brokerType: brokerType,
                    destination: destination,
                    message: messageContent
                })
            });

            if (response.status === 'SUCCESS') {
                window.app.showSuccess(`Сообщение отправлено через ${brokerType}! ID: ${response.messageId}`);
                this.addMessageToHistory({
                    timestamp: new Date().toISOString(),
                    brokerType: brokerType,
                    destination: destination,
                    message: messageContent,
                    status: 'SENT',
                    messageId: response.messageId
                });
            } else {
                window.app.showError('Не удалось отправить сообщение: ' + response.message);
            }

        } catch (error) {
            console.error('Error sending message:', error);
            window.app.showError('Не удалось отправить сообщение: ' + error.message);
        } finally {
            sendBtn.innerHTML = originalText;
            sendBtn.disabled = false;
        }
    }

    async subscribeToBroker() {
        const brokerType = document.getElementById('subscribeBroker').value;
        const destination = document.getElementById('subscribeDestination').value;

        if (!brokerType || !destination) {
            window.app.showError('Пожалуйста, выберите брокер и введите назначение');
            return;
        }

        const subscriptionKey = `${brokerType}:${destination}`;

        if (this.activeSubscriptions.has(subscriptionKey)) {
            window.app.showError('Уже подписаны на это назначение');
            return;
        }

        try {
            const response = await window.app.apiCall(`/messages/subscribe/${brokerType}?destination=${encodeURIComponent(destination)}`, {
                method: 'POST'
            });

            if (response.status === 'SUCCESS') {
                this.activeSubscriptions.add(subscriptionKey);
                this.updateSubscriptionsDisplay();
                window.app.showSuccess(`Подписаны на ${destination} через ${brokerType}`);
            } else {
                window.app.showError('Не удалось подписаться: ' + response.message);
            }

        } catch (error) {
            console.error('Error subscribing:', error);
            window.app.showError('Не удалось подписаться: ' + error.message);
        }
    }

    updateSubscriptionsDisplay() {
        const subscriptionsList = document.getElementById('subscriptionsList');

        if (this.activeSubscriptions.size === 0) {
            subscriptionsList.innerHTML = '<small class="text-muted">Нет активных подписок</small>';
            return;
        }

        let html = '';
        this.activeSubscriptions.forEach(subscription => {
            const [broker, destination] = subscription.split(':');
            html += `
                <div class="d-flex justify-content-between align-items-center mb-2 p-2 bg-white rounded">
                    <div>
                        <span class="badge bg-primary me-2">${broker}</span>
                        <small>${destination}</small>
                    </div>
                    <button class="btn btn-sm btn-outline-danger" onclick="messagesPage.unsubscribe('${subscription}')">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            `;
        });

        subscriptionsList.innerHTML = html;
    }

    unsubscribe(subscriptionKey) {
        this.activeSubscriptions.delete(subscriptionKey);
        this.updateSubscriptionsDisplay();
        window.app.showSuccess('Отписаны от назначения');
    }

    async loadMessageHistory() {
        try {
            const response = await window.app.apiCall('/messages/history');

            if (response.status === 'SUCCESS') {
                this.messageHistory = response.messages || [];
                this.updateHistoryDisplay();
            }
        } catch (error) {
            console.error('Error loading message history:', error);
        }
    }

    updateHistoryDisplay() {
        const tableBody = document.getElementById('messageHistoryTable');
        const historyCount = document.getElementById('historyCount');

        if (this.messageHistory.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted">
                        Сообщений пока нет. Отправьте сообщение, чтобы увидеть историю.
                    </td>
                </tr>
            `;
            historyCount.textContent = 'Показано 0 сообщений';
            return;
        }

        let html = '';
        this.messageHistory.forEach(msg => {
            const time = window.app.formatTimestamp(msg.timestamp);
            const statusBadge = msg.status === 'SENT'
                ? '<span class="badge bg-success">Отправлено</span>'
                : '<span class="badge bg-info">Получено</span>';

            html += `
                <tr>
                    <td><small>${time}</small></td>
                    <td><span class="badge bg-secondary">${msg.brokerType}</span></td>
                    <td><code>${msg.destination || 'N/A'}</code></td>
                    <td>
                        <div class="message-text">${this.escapeHtml(msg.message)}</div>
                        ${msg.messageId ? `<small class="text-muted">ID: ${msg.messageId}</small>` : ''}
                    </td>
                    <td>${statusBadge}</td>
                </tr>
            `;
        });

        tableBody.innerHTML = html;
        historyCount.textContent = `Показано ${this.messageHistory.length} сообщений`;
    }

    addMessageToHistory(message) {
        this.messageHistory.unshift(message);

        if (this.messageHistory.length > 100) {
            this.messageHistory = this.messageHistory.slice(0, 100);
        }

        this.updateHistoryDisplay();
    }

    async clearMessageHistory() {
        if (!confirm('Вы уверены, что хотите очистить всю историю сообщений?')) {
            return;
        }

        try {
            const response = await window.app.apiCall('/messages/history', {
                method: 'DELETE'
            });

            if (response.status === 'SUCCESS') {
                this.messageHistory = [];
                this.updateHistoryDisplay();
                window.app.showSuccess(`Очищено ${response.clearedMessages} сообщений из истории`);
            }
        } catch (error) {
            console.error('Error clearing history:', error);
            window.app.showError('Не удалось очистить историю: ' + error.message);
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

function initializeBrokers() {
    window.app.apiCall('/messages/initialize', { method: 'POST' })
        .then(data => {
            if (data.status === 'SUCCESS') {
                window.app.showSuccess('Брокеры успешно инициализированы!');
                setTimeout(() => location.reload(), 1000);
            } else {
                window.app.showError('Не удалось инициализировать брокеры: ' + data.message);
            }
        })
        .catch(error => {
            window.app.showError('Ошибка инициализации брокеров: ' + error.message);
        });
}

function quickSend(brokerType, message) {
    document.getElementById('brokerType').value = brokerType;
    document.getElementById('messageContent').value = message;
    document.getElementById('sendMessageForm').dispatchEvent(new Event('submit'));
}

function clearForm() {
    document.getElementById('sendMessageForm').reset();
    document.getElementById('destination').value = document.getElementById('destination').getAttribute('value') || '';
}

function subscribeToBroker() {
    if (window.messagesPage) {
        window.messagesPage.subscribeToBroker();
    }
}

function clearMessageHistory() {
    if (window.messagesPage) {
        window.messagesPage.clearMessageHistory();
    }
}

function refreshMessageHistory() {
    if (window.messagesPage) {
        window.messagesPage.loadMessageHistory();
        window.app.showSuccess('История сообщений обновлена');
    }
}

document.addEventListener('DOMContentLoaded', function() {
    window.messagesPage = new MessagesPage();
});
