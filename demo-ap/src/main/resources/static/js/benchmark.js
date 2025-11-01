class BenchmarkPage {
    constructor() {
        this.benchmarkResults = [];
        this.activeBenchmarks = new Map();
        this.init();
    }

    init() {
        console.log('Benchmark page initialized');
        this.setupEventListeners();
        this.updateMessageCountDisplay();
        this.loadActiveBenchmarks();

        // Auto-refresh active benchmarks every 5 seconds
        setInterval(() => this.loadActiveBenchmarks(), 5000);
    }

    setupEventListeners() {
        // Benchmark configuration form
        const benchmarkForm = document.getElementById('benchmarkConfigForm');
        if (benchmarkForm) {
            benchmarkForm.addEventListener('submit', (e) => this.handleRunBenchmark(e));
        }

        // Message count slider
        const messageCountSlider = document.getElementById('messageCount');
        if (messageCountSlider) {
            messageCountSlider.addEventListener('input', () => this.updateMessageCountDisplay());
        }

        // Test type change
        document.querySelectorAll('input[name="testType"]').forEach(radio => {
            radio.addEventListener('change', () => this.updateTestType());
        });
    }

    updateMessageCountDisplay() {
        const slider = document.getElementById('messageCount');
        const valueDisplay = document.getElementById('messageCountValue');
        if (slider && valueDisplay) {
            valueDisplay.textContent = `(${slider.value})`;
        }
    }

    updateTestType() {
        const isAsync = document.getElementById('asyncTest').checked;
        const runBtn = document.getElementById('runBenchmarkBtn');

        if (isAsync) {
            runBtn.innerHTML = '<i class="fas fa-play me-1"></i>Start Async Benchmark';
        } else {
            runBtn.innerHTML = '<i class="fas fa-play me-1"></i>Run Benchmark';
        }
    }

    async handleRunBenchmark(event) {
        event.preventDefault();

        const selectedBrokers = this.getSelectedBrokers();
        if (selectedBrokers.length === 0) {
            window.app.showError('Please select at least one broker to test');
            return;
        }

        const messageCount = parseInt(document.getElementById('messageCount').value);
        const destination = document.getElementById('destination').value || 'benchmark-queue';
        const isAsync = document.getElementById('asyncTest').checked;

        const benchmarkRequest = {
            brokers: selectedBrokers,
            messageCount: messageCount,
            destination: destination
        };

        const runBtn = document.getElementById('runBenchmarkBtn');
        const originalText = runBtn.innerHTML;

        try {
            runBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Running...';
            runBtn.disabled = true;

            let response;
            if (isAsync) {
                response = await this.startAsyncBenchmark(benchmarkRequest);
            } else {
                response = await this.runSyncBenchmark(benchmarkRequest);
            }

            if (response.status === 'SUCCESS') {
                if (isAsync) {
                    window.app.showSuccess(`Async benchmark started! ID: ${response.benchmarkId}`);
                    this.loadActiveBenchmarks();
                } else {
                    window.app.showSuccess(`Benchmark completed! Tested ${response.totalBrokersTested} brokers`);
                    this.displayResults(response.results);
                }
            } else {
                window.app.showError('Benchmark failed: ' + response.message);
            }

        } catch (error) {
            console.error('Error running benchmark:', error);
            window.app.showError('Failed to run benchmark: ' + error.message);
        } finally {
            runBtn.innerHTML = originalText;
            runBtn.disabled = false;
        }
    }

    getSelectedBrokers() {
        const selectedBrokers = [];
        document.querySelectorAll('.broker-checkbox:checked').forEach(checkbox => {
            selectedBrokers.push(checkbox.value);
        });
        return selectedBrokers;
    }

    async runSyncBenchmark(benchmarkRequest) {
        return await window.app.apiCall('/benchmark/run', {
            method: 'POST',
            body: JSON.stringify(benchmarkRequest)
        });
    }

    async startAsyncBenchmark(benchmarkRequest) {
        return await window.app.apiCall('/benchmark/start-async', {
            method: 'POST',
            body: JSON.stringify(benchmarkRequest)
        });
    }

    async loadActiveBenchmarks() {
        try {
            const response = await window.app.apiCall('/benchmark/status');

            if (response.status === 'SUCCESS') {
                this.activeBenchmarks = new Map(Object.entries(response.activeBenchmarks));
                this.updateActiveBenchmarksDisplay();
            }
        } catch (error) {
            console.error('Error loading active benchmarks:', error);
        }
    }

    updateActiveBenchmarksDisplay() {
        const activeSection = document.getElementById('activeBenchmarksSection');
        const benchmarksList = document.getElementById('activeBenchmarksList');
        const stopAllBtn = document.getElementById('stopAllBtn');

        if (this.activeBenchmarks.size === 0) {
            activeSection.style.display = 'none';
            stopAllBtn.disabled = true;
            return;
        }

        activeSection.style.display = 'block';
        stopAllBtn.disabled = false;

        let html = '';
        this.activeBenchmarks.forEach((isRunning, benchmarkId) => {
            const statusBadge = isRunning
                ? '<span class="badge bg-warning"><i class="fas fa-spinner fa-spin me-1"></i>Running</span>'
                : '<span class="badge bg-secondary"><i class="fas fa-check me-1"></i>Completed</span>';

            html += `
                <div class="d-flex justify-content-between align-items-center mb-2 p-2 bg-light rounded">
                    <div>
                        <code class="me-3">${benchmarkId}</code>
                        ${statusBadge}
                    </div>
                    <button class="btn btn-sm btn-outline-danger" onclick="benchmarkPage.stopBenchmark('${benchmarkId}')">
                        <i class="fas fa-stop me-1"></i>
                        Stop
                    </button>
                </div>
            `;
        });

        benchmarksList.innerHTML = html;
    }

    async stopBenchmark(benchmarkId) {
        try {
            const response = await window.app.apiCall(`/benchmark/stop/${benchmarkId}`, {
                method: 'POST'
            });

            if (response.status === 'SUCCESS') {
                window.app.showSuccess(`Benchmark ${response.stopped ? 'stopped' : 'could not be stopped'}`);
                this.loadActiveBenchmarks();
            }
        } catch (error) {
            console.error('Error stopping benchmark:', error);
            window.app.showError('Failed to stop benchmark: ' + error.message);
        }
    }

    async stopAllBenchmarks() {
        try {
            const response = await window.app.apiCall('/benchmark/stop-all', {
                method: 'POST'
            });

            if (response.status === 'SUCCESS') {
                window.app.showSuccess('All benchmarks stopped');
                this.loadActiveBenchmarks();
            }
        } catch (error) {
            console.error('Error stopping all benchmarks:', error);
            window.app.showError('Failed to stop benchmarks: ' + error.message);
        }
    }

    displayResults(results) {
        this.benchmarkResults = results;
        this.updateResultsTable();
        this.updateResultsSummary();
        this.renderCharts();
    }

    updateResultsTable() {
        const tableBody = document.getElementById('resultsTable');

        if (this.benchmarkResults.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center text-muted py-4">
                        <i class="fas fa-chart-bar fa-2x mb-3 d-block"></i>
                        No benchmark results yet. Run a test to see performance comparison.
                    </td>
                </tr>
            `;
            return;
        }

        let html = '';
        this.benchmarkResults.forEach(result => {
            const successRate = (result.successfulMessages / result.totalMessages * 100).toFixed(1);
            const throughput = result.messagesPerSecond.toFixed(2);
            const time = (result.totalTimeMs / 1000).toFixed(2);

            const statusBadge = result.status === 'SUCCESS'
                ? '<span class="badge bg-success"><i class="fas fa-check me-1"></i>Success</span>'
                : `<span class="badge bg-danger"><i class="fas fa-times me-1"></i>${result.status}</span>`;

            const throughputClass = this.getThroughputClass(throughput);
            const successRateClass = successRate >= 95 ? 'throughput-high' :
                                   successRate >= 80 ? 'throughput-medium' : 'throughput-low';

            html += `
                <tr>
                    <td>
                        <strong>${result.brokerType}</strong>
                    </td>
                    <td>${statusBadge}</td>
                    <td>${window.app.formatNumber(result.totalMessages)}</td>
                    <td>${window.app.formatNumber(result.successfulMessages)}</td>
                    <td>${time}s</td>
                    <td class="${throughputClass}">${throughput} msg/s</td>
                    <td class="${successRateClass}">${successRate}%</td>
                    <td>
                        <button class="btn btn-sm btn-outline-info" onclick="benchmarkPage.showDetailedStats('${result.brokerType}')">
                            <i class="fas fa-chart-bar me-1"></i>Details
                        </button>
                    </td>
                </tr>
            `;
        });

        tableBody.innerHTML = html;
    }

    getThroughputClass(throughput) {
        const tps = parseFloat(throughput);
        if (tps > 500) return 'throughput-high';
        if (tps > 100) return 'throughput-medium';
        return 'throughput-low';
    }

    updateResultsSummary() {
        const summaryDiv = document.getElementById('resultsSummary');

        if (this.benchmarkResults.length === 0) {
            summaryDiv.style.display = 'none';
            return;
        }

        // Calculate summary statistics
        const successfulTests = this.benchmarkResults.filter(r => r.status === 'SUCCESS').length;
        const totalTests = this.benchmarkResults.length;
        const bestPerformer = this.findBestPerformer();

        summaryDiv.style.display = 'block';
        summaryDiv.innerHTML = `
            <div class="alert alert-info">
                <div class="row">
                    <div class="col-md-4">
                        <strong>Tests Completed:</strong> ${successfulTests}/${totalTests}
                    </div>
                    <div class="col-md-4">
                        <strong>Best Performer:</strong> ${bestPerformer ? bestPerformer.brokerType : 'N/A'}
                    </div>
                    <div class="col-md-4">
                        <strong>Highest Throughput:</strong> ${bestPerformer ? bestPerformer.messagesPerSecond.toFixed(2) + ' msg/s' : 'N/A'}
                    </div>
                </div>
            </div>
        `;
    }

    findBestPerformer() {
        return this.benchmarkResults
            .filter(r => r.status === 'SUCCESS')
            .sort((a, b) => b.messagesPerSecond - a.messagesPerSecond)[0];
    }

    renderCharts() {
        const chartsSection = document.getElementById('chartsSection');

        if (this.benchmarkResults.length === 0) {
            chartsSection.style.display = 'none';
            return;
        }

        chartsSection.style.display = 'block';

        // Simple bar chart using HTML/CSS (can be enhanced with Chart.js later)
        this.renderSimpleBarChart('throughputChart', 'Throughput (msg/s)', 'messagesPerSecond');
        this.renderSimpleBarChart('successRateChart', 'Success Rate (%)', 'successRate');
    }

    renderSimpleBarChart(chartId, title, dataField) {
        const chartDiv = document.getElementById(chartId);
        const successfulResults = this.benchmarkResults.filter(r => r.status === 'SUCCESS');

        if (successfulResults.length === 0) {
            chartDiv.innerHTML = `<div class="text-muted text-center py-4">No successful results to display</div>`;
            return;
        }

        let html = '<div class="chart-bars">';

        successfulResults.forEach(result => {
            let value;
            if (dataField === 'successRate') {
                value = (result.successfulMessages / result.totalMessages * 100);
            } else {
                value = result[dataField];
            }

            const percentage = (value / this.getMaxValue(successfulResults, dataField)) * 100;
            const barColor = dataField === 'successRate' ?
                (value >= 95 ? 'bg-success' : value >= 80 ? 'bg-warning' : 'bg-danger') :
                'bg-primary';

            html += `
                <div class="chart-bar mb-2">
                    <div class="d-flex justify-content-between mb-1">
                        <small>${result.brokerType}</small>
                        <small>${value.toFixed(2)}</small>
                    </div>
                    <div class="progress" style="height: 20px;">
                        <div class="progress-bar ${barColor}"
                             role="progressbar"
                             style="width: ${percentage}%"
                             aria-valuenow="${value}"
                             aria-valuemin="0"
                             aria-valuemax="100">
                        </div>
                    </div>
                </div>
            `;
        });

        html += '</div>';
        chartDiv.innerHTML = html;
    }

    getMaxValue(results, dataField) {
        return Math.max(...results.map(r => {
            if (dataField === 'successRate') {
                return (r.successfulMessages / r.totalMessages * 100);
            }
            return r[dataField];
        }));
    }

    showDetailedStats(brokerType) {
        const result = this.benchmarkResults.find(r => r.brokerType === brokerType);
        if (!result) return;

        const details = `
Broker: ${result.brokerType}
Status: ${result.status}
Total Messages: ${window.app.formatNumber(result.totalMessages)}
Successful: ${window.app.formatNumber(result.successfulMessages)}
Failed: ${window.app.formatNumber(result.totalMessages - result.successfulMessages)}
Total Time: ${(result.totalTimeMs / 1000).toFixed(2)} seconds
Throughput: ${result.messagesPerSecond.toFixed(2)} messages/second
Success Rate: ${((result.successfulMessages / result.totalMessages) * 100).toFixed(1)}%
        `.trim();

        alert(details);
    }
}

// Global functions
function initializeBrokers() {
    window.app.apiCall('/messages/initialize', { method: 'POST' })
        .then(data => {
            if (data.status === 'SUCCESS') {
                window.app.showSuccess('Brokers initialized successfully!');
                setTimeout(() => location.reload(), 1000);
            } else {
                window.app.showError('Failed to initialize brokers: ' + data.message);
            }
        })
        .catch(error => {
            window.app.showError('Error initializing brokers: ' + error.message);
        });
}

function runQuickBenchmark() {
    // Select all connected brokers
    document.querySelectorAll('.broker-checkbox').forEach(checkbox => {
        checkbox.checked = !checkbox.disabled;
    });

    // Set quick test parameters
    document.getElementById('messageCount').value = 100;
    document.getElementById('destination').value = 'benchmark-queue';
    document.getElementById('syncTest').checked = true;

    // Update display
    if (window.benchmarkPage) {
        window.benchmarkPage.updateMessageCountDisplay();
        window.benchmarkPage.updateTestType();
    }

    // Submit form
    document.getElementById('benchmarkConfigForm').dispatchEvent(new Event('submit'));
}

function stopAllBenchmarks() {
    if (window.benchmarkPage) {
        window.benchmarkPage.stopAllBenchmarks();
    }
}

function refreshResults() {
    window.app.showSuccess('Results refreshed');
    // In a real app, this would reload results from server
}

function clearResults() {
    if (confirm('Are you sure you want to clear all benchmark results?')) {
        if (window.benchmarkPage) {
            window.benchmarkPage.benchmarkResults = [];
            window.benchmarkPage.updateResultsTable();
            window.benchmarkPage.updateResultsSummary();
            document.getElementById('chartsSection').style.display = 'none';
        }
        window.app.showSuccess('Results cleared');
    }
}

// Initialize benchmark page when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.benchmarkPage = new BenchmarkPage();
});