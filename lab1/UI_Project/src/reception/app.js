export function initPage() {
  const form = document.getElementById('create-patient-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const body = {
      name: document.getElementById('name').value,
      surname: document.getElementById('surname').value,
      birthdate: document.getElementById('birthdate').value
    };

    try {
      const res = await fetch('/api/patients', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      if (!res.ok) {
        const errorText = await res.text();
        alert(`Ошибка при создании пациента: ${res.status} - ${errorText}`);
      } else {
        const result = await res.json();
        alert('Пациент успешно создан');
        console.log('Создан пациент:', result);
      }
    } catch (err) {
      alert('Ошибка соединения: ' + err.message);
      console.error('Ошибка:', err);
    }
  });

  deleteForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const patientId = document.getElementById('patientId').value;

    try {
      const res = await fetch(`https://localhost:8082/reception/patients?patientId=${patientId}`, {
        method: 'DELETE'
      });

      if (!res.ok) {
        const err = await res.json();
        alert('Ошибка: ' + err.message);
      } else {
        alert('Пациент удалён');
        deleteForm.reset();
      }
    } catch (err) {
      alert('Ошибка соединения: ' + err);
    }
  });
}
