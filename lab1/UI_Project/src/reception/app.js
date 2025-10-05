export function initPage() {
    const createForm = document.getElementById('create-patient-form');
    const deleteForm = document.getElementById('delete-patient-form');

    createForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const body = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            birthDate: document.getElementById('birthDate').value
        };

        try {
            const res = await fetch('http://localhost:8082/reception/patients', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            if (!res.ok) {
                const err = await res.json();
                alert('Ошибка: ' + err.message);
            } else {
                alert('Пациент успешно создан');
                createForm.reset();
            }
        } catch (err) {
            alert('Ошибка соединения: ' + err);
        }
    });

    deleteForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const patientId = document.getElementById('patientId').value;

        try {
            const res = await fetch(`http://localhost:8082/reception/patients?patientId=${patientId}`, {
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
