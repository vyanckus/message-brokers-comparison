// Common JavaScript functions for the application

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