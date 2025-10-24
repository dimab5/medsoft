export function initPage() {
    const tbody = document.querySelector('#visits-table tbody');
    let ws;
    let visits = [];

    function renderTable() {
        tbody.innerHTML = '';
        visits.forEach(v => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${v.id}</td>
                <td>${v.name || '-'}</td>
                <td>${v.surname || '-'}</td>
                <td>${v.status}</td>
            `;
            tbody.appendChild(row);
        });
    }

    async function loadInitialVisits() {
        try {
            console.log('Loading initial visits from API...');
            const res = await fetch('/api/visits');
            if (res.ok) {
                visits = await res.json();
                renderTable();
                console.log('Initial visits loaded:', visits.length);
            } else {
                console.error('Failed to load initial visits:', res.status);
            }
        } catch (err) {
            console.error('Error loading initial visits:', err);
        }
    }

    function connectWebSocket() {
        try {
            ws = new WebSocket('ws://localhost:8083/ws/visits');

            ws.onopen = () => {
                console.log('Connected to Doctor WebSocket');
            };

            ws.onmessage = (event) => {
                const visit = JSON.parse(event.data);
                console.log('WebSocket update received:', visit);

                const idx = visits.findIndex(v => v.id === visit.id);
                if (idx >= 0) {
                    visits[idx] = visit;
                    console.log('Updated existing visit:', visit.id);
                } else {
                    visits.push(visit);
                    console.log('Added new visit:', visit.id);
                }
                renderTable();
            };

            ws.onclose = () => {
                console.log('Doctor WS disconnected');
                setTimeout(() => {
                    console.log('Attempting to reconnect...');
                    connectWebSocket();
                }, 5000);
            };

            ws.onerror = (error) => {
                console.error('WebSocket error:', error);
            };
        } catch (err) {
            console.error('Failed to create WebSocket:', err);
        }
    }

    async function initialize() {
        await loadInitialVisits();
        connectWebSocket();
    }

    const startBtn = document.getElementById('startBtn');
    const finishBtn = document.getElementById('finishBtn');

    startBtn?.addEventListener('click', () => updateVisitStatus('start'));
    finishBtn?.addEventListener('click', () => updateVisitStatus('finish'));

    async function updateVisitStatus(action) {
        const visitId = document.getElementById('visitId').value.trim();
        if (!visitId) return alert('Введите UUID визита');

        try {
            const res = await fetch(`http://localhost:8083/api/visits/${visitId}/${action}`, { method: 'POST' });
            if (res.ok) {
                alert(`✅ Визит ${action === 'start' ? 'начат' : 'завершён'}`);
                await loadInitialVisits();
            } else {
                alert(`Ошибка: ${res.status}`);
            }
        } catch (err) {
            alert('Ошибка соединения: ' + err.message);
        }
    }

    initialize();
}