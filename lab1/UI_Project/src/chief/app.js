export function initPage() {
  const tbody = document.querySelector('#patients-table tbody');
  const ws = new WebSocket('ws://localhost:8082/ws/patients');

  let patients = [];

  function renderTable() {
    tbody.innerHTML = '';
    patients.forEach(p => {
      const row = document.createElement('tr');
      row.innerHTML = `<td>${p.firstName}</td><td>${p.lastName}</td><td>${p.birthDate}</td>`;
      tbody.appendChild(row);
    });
  }

  ws.onopen = () => {
    console.log('Connected to HIS WebSocket');
    ws.send(JSON.stringify({ action: 'GET_ALL' }));
  };

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);

    if (Array.isArray(data)) {
      patients = data;
      renderTable();
    }
    else if (data.action === 'CREATE') {
      patients.push(data);
      renderTable();
    } else if (data.action === 'DELETE') {
      patients = patients.filter(p => p.id !== data.id);
      renderTable();
    }
  };

  ws.onclose = () => console.log('Disconnected');
}
