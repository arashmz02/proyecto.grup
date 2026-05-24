function inicializarDashboard(contextPath) {
    const base = contextPath + '/reportes/dashboard?tipo=json&metrica=';

    fetch(base + 'porMes')
        .then(r => r.json())
        .then(data => {
            const ctx = document.getElementById('graficoPorMes');
            if (!ctx) return;
            new Chart(ctx.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: data.labels || [],
                    datasets: [{
                        label: 'Certificaciones',
                        data: data.values || [],
                        backgroundColor: '#003d7a'
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { display: false },
                        title: {
                            display: true,
                            text: 'Certificaciones por mes (ultimo anno)'
                        }
                    },
                    scales: {
                        y: { beginAtZero: true, ticks: { stepSize: 1 } }
                    }
                }
            });
        })
        .catch(err => mostrarError('graficoPorMes', err));

    fetch(base + 'porSector')
        .then(r => r.json())
        .then(data => {
            const ctx = document.getElementById('graficoPorSector');
            if (!ctx) return;
            new Chart(ctx.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: data.labels || [],
                    datasets: [{
                        data: data.values || [],
                        backgroundColor: [
                            '#003d7a','#0056b3','#1976d2',
                            '#42a5f5','#28a745','#ffc107'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { position: 'right' },
                        title: {
                            display: true,
                            text: 'Certificaciones por sector'
                        }
                    }
                }
            });
        })
        .catch(err => mostrarError('graficoPorSector', err));

    fetch(base + 'porAnio')
        .then(r => r.json())
        .then(data => {
            const ctx = document.getElementById('graficoPorAnio');
            if (!ctx) return;
            new Chart(ctx.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: data.labels || [],
                    datasets: [{
                        label: 'Folios',
                        data: data.values || [],
                        backgroundColor: '#28a745'
                    }]
                },
                options: {
                    indexAxis: 'y',
                    responsive: true,
                    plugins: {
                        legend: { display: false },
                        title: {
                            display: true,
                            text: 'Folios por anno'
                        }
                    },
                    scales: {
                        x: { beginAtZero: true, ticks: { stepSize: 1 } }
                    }
                }
            });
        })
        .catch(err => mostrarError('graficoPorAnio', err));
}

function mostrarError(canvasId, err) {
    const el = document.getElementById(canvasId);
    if (el && el.parentElement) {
        el.parentElement.innerHTML =
            '<div style="padding:20px;color:#dc3545;text-align:center;">' +
            'No se pudieron cargar los datos del grafico.</div>';
    }
    console.error('Error grafico ' + canvasId + ':', err);
}