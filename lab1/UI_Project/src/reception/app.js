export function initPage() {
  const form = document.getElementById('create-patient-form');
  const deleteForm = document.getElementById('delete-patient-form');

    form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const body = {
      name: document.getElementById('name').value,
      surname: document.getElementById('surname').value,
      birthdate: document.getElementById('birthdate').value
    };

    try {
      const res = await fetch('/api/reception/patients', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

        if (!res.ok) {
            const text = await res.text();
            alert(`Ошибка при создании пациента: ${res.status} - ${text}`);
            return;
        }

        alert('✅ Пациент успешно создан');
        form.reset();
    } catch (err) {
      alert('Ошибка соединения: ' + err.message);
      console.error('Ошибка:', err);
    }
  });

  deleteForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const patientId = document.getElementById('patientId').value;

    try {
        const res = await fetch(`/api/reception/patients/${patientId}`, {
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

    const visitForm = document.getElementById('create-visit-form');

    visitForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const visitTime = new Date().toISOString();
        const patientId = document.getElementById('visitPatientId').value;

        try {
            const res = await fetch('api/reception/visits', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    patientId: patientId,
                    visitTime: visitTime
                })
            });

            if (res.ok) {
                alert('✅ Визит зарегистрирован');
                visitForm.reset();
            } else {
                alert('Ошибка при регистрации визита: ' + res.status);
            }
        } catch (err) {
            alert('Ошибка соединения: ' + err.message);
        }
    });

}
