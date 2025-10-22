export function initPage() {
    const tbody = document.querySelector('#visits-table tbody');
    const ws = new WebSocket('wss://localhost:8083/ws/visits'); // порт, где doctorApi, потом поправить

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

    ws.onopen = () => {
        console.log('Connected to Doctor WebSocket');
    };

    ws.onmessage = (event) => {
        const visit = JSON.parse(event.data);
        const idx = visits.findIndex(v => v.id === visit.id);
        if (idx >= 0) visits[idx] = visit;
        else visits.push(visit);
        renderTable();
    };

    ws.onclose = () => console.log('Doctor WS disconnected');

    const startBtn = document.getElementById('startBtn');
    const finishBtn = document.getElementById('finishBtn');

    startBtn.addEventListener('click', () => updateVisitStatus('start'));
    finishBtn.addEventListener('click', () => updateVisitStatus('finish'));

    async function updateVisitStatus(action) {
        const visitId = document.getElementById('visitId').value.trim();
        if (!visitId) return alert('Введите UUID визита');

        try {
            const res = await fetch(`/api/visits/${visitId}/${action}`, { method: 'POST' });
            if (res.ok) alert(`✅ Визит ${action === 'start' ? 'начат' : 'завершён'}`);
            else alert(`Ошибка: ${res.status}`);
        } catch (err) {
            alert('Ошибка соединения: ' + err.message);
        }
    }
}
