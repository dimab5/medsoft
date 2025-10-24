export function initPage() {
  const tbody = document.querySelector('#patients-table tbody');
  const ws = new WebSocket('wss://localhost:8081/ws/patients');

  let patients = [];

  function renderTable() {
    tbody.innerHTML = '';
    patients.forEach(p => {
      const row = document.createElement('tr');
      row.innerHTML = `<td>${p.name}</td><td>${p.surname}</td><td>${p.birthdate}</td><td>${p.status ?? '-'}</td>`;
      tbody.appendChild(row);
    });
  }

  ws.onopen = () => {
    console.log('Connected to HIS WebSocket');
    ws.send(JSON.stringify({ action: 'GET_ALL' }));
  };

  ws.onmessage = (event) => {
    console.log("WS message:", event.data);
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
    } else if (data.action === 'UPDATE') {
      const patientIndex = patients.findIndex(p => p.id === data.id);
      if (patientIndex !== -1) {
        patients[patientIndex].status = data.visitStatus;
        renderTable();
      } else {
        console.warn('Patient not found for UPDATE:', data.id);
      }
    }
  };

  ws.onclose = () => console.log('Disconnected');
}