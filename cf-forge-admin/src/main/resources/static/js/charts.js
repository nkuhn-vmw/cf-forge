// CF Forge Admin — Chart.js initialization
document.addEventListener('DOMContentLoaded', function() {
    var chartColors = {
        accent: '#00d4aa',
        accentHover: '#00f0c0',
        success: '#2ed573',
        error: '#ff4757',
        warn: '#ffa502',
        info: '#58a6ff',
        surface: '#161d2e',
        border: '#1e2a3f',
        text: '#6b7a94',
        gridLine: 'rgba(30,42,63,0.5)',
    };

    var palette = [
        chartColors.accent,
        chartColors.info,
        chartColors.success,
        chartColors.warn,
        chartColors.error,
        '#a78bfa',
        '#f472b6',
    ];

    // Common chart defaults
    Chart.defaults.color = chartColors.text;
    Chart.defaults.borderColor = chartColors.gridLine;
    Chart.defaults.font.family = '"IBM Plex Mono", "SF Mono", monospace';
    Chart.defaults.font.size = 11;

    // Build Status Distribution (doughnut)
    var buildEl = document.getElementById('buildStatusChart');
    if (buildEl && typeof buildByStatus !== 'undefined') {
        var labels = Object.keys(buildByStatus);
        var data = Object.values(buildByStatus);
        var colors = labels.map(function(label) {
            if (label === 'SUCCESS') return chartColors.success;
            if (label === 'FAILED') return chartColors.error;
            if (label === 'BUILDING') return chartColors.warn;
            if (label === 'QUEUED') return chartColors.info;
            return chartColors.surface;
        });

        new Chart(buildEl, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderColor: '#0f1420',
                    borderWidth: 2,
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'right', labels: { padding: 16 } },
                }
            }
        });
    }

    // Deployment by Strategy (bar)
    var strategyEl = document.getElementById('strategyChart');
    if (strategyEl && typeof deployByStrategy !== 'undefined') {
        new Chart(strategyEl, {
            type: 'bar',
            data: {
                labels: Object.keys(deployByStrategy),
                datasets: [{
                    label: 'Deployments',
                    data: Object.values(deployByStrategy),
                    backgroundColor: palette.slice(0, Object.keys(deployByStrategy).length),
                    borderRadius: 4,
                    barPercentage: 0.6,
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { color: chartColors.gridLine } },
                    x: { grid: { display: false } },
                }
            }
        });
    }

    // Deployment by Environment (bar)
    var envEl = document.getElementById('envChart');
    if (envEl && typeof deployByEnvironment !== 'undefined') {
        new Chart(envEl, {
            type: 'bar',
            data: {
                labels: Object.keys(deployByEnvironment),
                datasets: [{
                    label: 'Deployments',
                    data: Object.values(deployByEnvironment),
                    backgroundColor: [chartColors.warn, chartColors.success, chartColors.info],
                    borderRadius: 4,
                    barPercentage: 0.6,
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { color: chartColors.gridLine } },
                    x: { grid: { display: false } },
                }
            }
        });
    }

    // Prompts chart (line chart fetched via API)
    var promptsEl = document.getElementById('promptsChart');
    if (promptsEl) {
        fetch('/api/v1/admin/metrics/timeseries?metric=agent.generate&granularity=HOURLY&days=7')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!Array.isArray(data) || data.length === 0) {
                    promptsEl.parentElement.insertAdjacentHTML('beforeend',
                        '<p style="color:#4a5568;font-size:13px;text-align:center;">No timeseries data available</p>');
                    return;
                }
                var labels = data.map(function(s) {
                    return new Date(s.periodStart).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
                });
                var counts = data.map(function(s) { return s.count; });

                new Chart(promptsEl, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Prompts',
                            data: counts,
                            borderColor: chartColors.accent,
                            backgroundColor: 'rgba(0,212,170,0.1)',
                            fill: true,
                            tension: 0.3,
                            pointRadius: 2,
                            pointHoverRadius: 5,
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: { beginAtZero: true, grid: { color: chartColors.gridLine } },
                            x: { grid: { display: false } },
                        }
                    }
                });
            })
            .catch(function() {
                promptsEl.parentElement.insertAdjacentHTML('beforeend',
                    '<p style="color:#4a5568;font-size:13px;text-align:center;">No timeseries data available</p>');
            });
    }

    // GenAI Request Volume (line chart fetched via API)
    var genaiEl = document.getElementById('genaiChart');
    if (genaiEl) {
        fetch('/api/v1/admin/metrics/timeseries?metric=genai.request&granularity=HOURLY&days=7')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!Array.isArray(data) || data.length === 0) {
                    genaiEl.parentElement.insertAdjacentHTML('beforeend',
                        '<p style="color:#4a5568;font-size:13px;text-align:center;">No timeseries data available</p>');
                    return;
                }
                var labels = data.map(function(s) {
                    return new Date(s.periodStart).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
                });
                var counts = data.map(function(s) { return s.count; });

                new Chart(genaiEl, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Requests',
                            data: counts,
                            borderColor: chartColors.info,
                            backgroundColor: 'rgba(88,166,255,0.1)',
                            fill: true,
                            tension: 0.3,
                            pointRadius: 2,
                            pointHoverRadius: 5,
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: { beginAtZero: true, grid: { color: chartColors.gridLine } },
                            x: { grid: { display: false } },
                        }
                    }
                });
            })
            .catch(function() {
                genaiEl.parentElement.insertAdjacentHTML('beforeend',
                    '<p style="color:#4a5568;font-size:13px;text-align:center;">No timeseries data available</p>');
            });
    }
});
