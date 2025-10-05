export function initPage() {
  const form = document.getElementById('create-patient-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = {
      firstName: document.getElementById('firstName').value,
      lastName: document.getElementById('lastName').value,
      birthDate: document.getElementById('birthDate').value
    };
    try {
      const res = await fetch('http://localhost:8081/reception/patients', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (!res.ok) {
        const err = await res.json();
        alert('Ошибка: ' + err.message);
      } else {
        alert('Пациент успешно создан');
      }
    } catch (err) {
      alert('Ошибка соединения: ' + err);
    }
  });
}
