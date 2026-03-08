// Глобальные переменные
let localStream;
let peerConnection;
let stompClient;
let userId;
let remoteUserId;
let isVideoEnabled = true;
let isAudioEnabled = true;
let isScreenSharing = false;
let screenStream;

// Конфигурация ICE серверов
const configuration = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' },
        { urls: 'stun:stun3.l.google.com:19302' },
        { urls: 'stun:stun4.l.google.com:19302' }
    ]
};

// Функция для показа уведомлений
function showNotification(message, type = 'info') {
    const area = document.getElementById('notificationArea');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    area.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Подключение к конференции
async function connect() {
    userId = document.getElementById('userId').value.trim();
    remoteUserId = document.getElementById('remoteUserId').value.trim();

    if (!userId || !remoteUserId) {
        showNotification('Пожалуйста, введите ID участников', 'error');
        return;
    }

    if (userId === remoteUserId) {
        showNotification('ID участников должны быть разными', 'error');
        return;
    }

    try {
        // Получаем доступ к камере и микрофону
        localStream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: true
        });

        document.getElementById('localVideo').srcObject = localStream;

        // Переключаем экраны
        document.getElementById('connectionScreen').style.display = 'none';
        document.getElementById('conferenceScreen').style.display = 'block';

        // Отображаем ID
        document.getElementById('localId').textContent = userId;
        document.getElementById('remoteId').textContent = remoteUserId;

        // Подключаемся к signaling серверу
        connectToSignalingServer();

        showNotification('Подключение к конференции...', 'info');

    } catch (error) {
        console.error('Ошибка доступа к медиаустройствам:', error);
        showNotification('Не удалось получить доступ к камере и микрофону', 'error');
    }
}

// Подключение к signaling серверу
function connectToSignalingServer() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Подключено к signaling серверу');
        document.getElementById('connectionStatus').textContent = '🟢 В сети';

        // Подписываемся на сигналы для текущего пользователя
        stompClient.subscribe('/topic/signal/' + userId, function(message) {
            const signal = JSON.parse(message.body);
            handleSignal(signal);
        });

        // Подписываемся на уведомления о присоединении
        stompClient.subscribe('/topic/join', function(message) {
            const signal = JSON.parse(message.body);
            console.log('Пользователь присоединился:', signal.from);
            if (signal.from === remoteUserId) {
                showNotification('Собеседник подключился!', 'success');
            }
        });

        // Подписываемся на уведомления об уходе
        stompClient.subscribe('/topic/leave', function(message) {
            const signal = JSON.parse(message.body);
            console.log('Пользователь покинул конференцию:', signal.from);
            if (signal.from === remoteUserId) {
                showNotification('Собеседник покинул конференцию', 'error');
                document.getElementById('remoteVideo').srcObject = null;
                document.getElementById('remoteStatus').textContent = 'Собеседник отключился';
            }
        });

        // Отправляем уведомление о присоединении
        stompClient.send("/app/join", {}, JSON.stringify({
            type: 'join',
            from: userId,
            to: 'all',
            data: userId + ' присоединился'
        }));

        // Создаем peer connection
        createPeerConnection();

    }, function(error) {
        console.error('Ошибка подключения к signaling серверу:', error);
        showNotification('Ошибка подключения к серверу', 'error');
        document.getElementById('connectionStatus').textContent = '🔴 Ошибка';
    });
}

// Создание peer connection
function createPeerConnection() {
    peerConnection = new RTCPeerConnection(configuration);

    // Добавляем локальные треки
    localStream.getTracks().forEach(track => {
        peerConnection.addTrack(track, localStream);
    });

    // Обработка ICE кандидатов
    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            sendSignal('ice-candidate', {
                candidate: event.candidate
            });
        }
    };

    // Обработка удаленного потока
    peerConnection.ontrack = event => {
        console.log('Получен удаленный поток');
        const remoteVideo = document.getElementById('remoteVideo');
        if (remoteVideo.srcObject !== event.streams[0]) {
            remoteVideo.srcObject = event.streams[0];
            document.getElementById('remoteStatus').textContent = '🟢 В сети';
            showNotification('Получено видео от собеседника', 'success');
        }
    };

    // Обработка состояния соединения
    peerConnection.onconnectionstatechange = event => {
        console.log('Состояние соединения:', peerConnection.connectionState);
        const status = document.getElementById('connectionStatus');

        switch(peerConnection.connectionState) {
            case 'connected':
                status.textContent = '🟢 Соединение установлено';
                break;
            case 'disconnected':
                status.textContent = '🟡 Соединение потеряно';
                document.getElementById('remoteVideo').srcObject = null;
                break;
            case 'failed':
                status.textContent = '🔴 Ошибка соединения';
                break;
            case 'closed':
                status.textContent = '⚪ Соединение закрыто';
                break;
        }
    };

    // Если мы инициируем звонок (по алфавиту), создаем offer
    if (userId < remoteUserId) {
        setTimeout(() => makeCall(), 1000);
    }
}

// Создание offer
async function makeCall() {
    try {
        const offer = await peerConnection.createOffer({
            offerToReceiveVideo: true,
            offerToReceiveAudio: true
        });
        await peerConnection.setLocalDescription(offer);

        sendSignal('offer', {
            sdp: peerConnection.localDescription
        });

        console.log('Отправлен offer');
    } catch (error) {
        console.error('Ошибка создания offer:', error);
        showNotification('Ошибка создания offer', 'error');
    }
}

// Обработка входящих сигналов
async function handleSignal(signal) {
    console.log('Получен сигнал:', signal.type);

    try {
        switch (signal.type) {
            case 'offer':
                await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
                const answer = await peerConnection.createAnswer();
                await peerConnection.setLocalDescription(answer);
                sendSignal('answer', {
                    sdp: peerConnection.localDescription
                });
                console.log('Отправлен answer');
                break;

            case 'answer':
                await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
                console.log('Получен answer');
                break;

            case 'ice-candidate':
                if (signal.data.candidate) {
                    await peerConnection.addIceCandidate(new RTCIceCandidate(signal.data.candidate));
                    console.log('Добавлен ICE кандидат');
                }
                break;

            case 'chat':
                displayMessage('remote', signal.data.text);
                break;

            case 'screen-share-start':
                showNotification('Собеседник начал демонстрацию экрана', 'info');
                document.getElementById('remoteStatus').textContent = '🖥️ Демонстрация экрана';
                break;

            case 'screen-share-stop':
                showNotification('Собеседник завершил демонстрацию экрана', 'info');
                document.getElementById('remoteStatus').textContent = '🟢 В сети';
                break;
        }
    } catch (error) {
        console.error('Ошибка обработки сигнала:', error);
    }
}

// Отправка сигнала
function sendSignal(type, data) {
    if (stompClient && stompClient.connected) {
        const signal = {
            type: type,
            from: userId,
            to: remoteUserId,
            data: data
        };
        stompClient.send("/app/signal", {}, JSON.stringify(signal));
    }
}

// Управление видео
function toggleVideo() {
    isVideoEnabled = !isVideoEnabled;
    if (localStream) {
        localStream.getVideoTracks().forEach(track => {
            track.enabled = isVideoEnabled;
        });
    }

    const btn = document.getElementById('toggleVideoBtn');
    const indicator = document.getElementById('videoIndicator');

    if (isVideoEnabled) {
        btn.innerHTML = '<span class="icon">📹</span><span class="text">Выкл видео</span>';
        indicator.textContent = '📹';
        indicator.style.opacity = '1';
    } else {
        btn.innerHTML = '<span class="icon">🚫</span><span class="text">Вкл видео</span>';
        indicator.textContent = '🚫';
        indicator.style.opacity = '0.5';
    }
}

// Управление аудио
function toggleAudio() {
    isAudioEnabled = !isAudioEnabled;
    if (localStream) {
        localStream.getAudioTracks().forEach(track => {
            track.enabled = isAudioEnabled;
        });
    }

    const btn = document.getElementById('toggleAudioBtn');
    const indicator = document.getElementById('audioIndicator');

    if (isAudioEnabled) {
        btn.innerHTML = '<span class="icon">🎤</span><span class="text">Выкл микрофон</span>';
        indicator.textContent = '🎤';
        indicator.style.opacity = '1';
    } else {
        btn.innerHTML = '<span class="icon">🔇</span><span class="text">Вкл микрофон</span>';
        indicator.textContent = '🔇';
        indicator.style.opacity = '0.5';
    }
}

// Регулировка громкости
function setVolume(value) {
    const remoteVideo = document.getElementById('remoteVideo');
    if (remoteVideo) {
        remoteVideo.volume = parseFloat(value);
        document.getElementById('volumeValue').textContent = Math.round(value * 100) + '%';
    }
}

// Демонстрация экрана
async function toggleScreenShare() {
    const btn = document.getElementById('screenShareBtn');
    const indicator = document.getElementById('screenIndicator');

    if (!isScreenSharing) {
        try {
            screenStream = await navigator.mediaDevices.getDisplayMedia({
                video: {
                    cursor: 'always'
                },
                audio: true
            });

            // Заменяем видео трек на трек экрана
            const videoTrack = screenStream.getVideoTracks()[0];
            const sender = peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');

            if (sender) {
                await sender.replaceTrack(videoTrack);
            }

            // Добавляем аудио трек с экрана если есть
            const audioTracks = screenStream.getAudioTracks();
            if (audioTracks.length > 0) {
                const audioSender = peerConnection.getSenders().find(s => s.track && s.track.kind === 'audio');
                if (audioSender) {
                    await audioSender.replaceTrack(audioTracks[0]);
                }
            }

            // Обработка остановки демонстрации
            videoTrack.onended = () => {
                stopScreenShare();
            };

            isScreenSharing = true;
            btn.innerHTML = '<span class="icon">⏹️</span><span class="text">Остановить</span>';
            indicator.style.display = 'inline';

            // Уведомляем собеседника
            sendSignal('screen-share-start', {});
            showNotification('Демонстрация экрана начата', 'success');

        } catch (error) {
            console.error('Ошибка демонстрации экрана:', error);
            showNotification('Ошибка демонстрации экрана', 'error');
        }
    } else {
        stopScreenShare();
    }
}

// Остановка демонстрации экрана
async function stopScreenShare() {
    if (screenStream) {
        screenStream.getTracks().forEach(track => track.stop());
    }

    // Возвращаем видео с камеры
    const videoTrack = localStream.getVideoTracks()[0];
    const sender = peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');

    if (sender) {
        await sender.replaceTrack(videoTrack);
    }

    // Возвращаем аудио с микрофона
    const audioTrack = localStream.getAudioTracks()[0];
    const audioSender = peerConnection.getSenders().find(s => s.track && s.track.kind === 'audio');

    if (audioSender) {
        await audioSender.replaceTrack(audioTrack);
    }

    isScreenSharing = false;

    const btn = document.getElementById('screenShareBtn');
    btn.innerHTML = '<span class="icon">🖥️</span><span class="text">Демонстрация</span>';
    document.getElementById('screenIndicator').style.display = 'none';

    // Уведомляем собеседника
    sendSignal('screen-share-stop', {});
    showNotification('Демонстрация экрана завершена', 'info');
}

// Отправка сообщения в чат
function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();

    if (message) {
        // Отображаем свое сообщение
        displayMessage('me', message);

        // Отправляем сообщение собеседнику
        sendSignal('chat', {
            text: message,
            timestamp: new Date().toISOString()
        });

        input.value = '';
    }
}

// Отображение сообщения в чате
function displayMessage(sender, text) {
    const messagesDiv = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender === 'me' ? 'my-message' : 'remote-message'}`;

    const time = new Date().toLocaleTimeString('ru-RU', {
        hour: '2-digit',
        minute: '2-digit'
    });

    messageDiv.innerHTML = `
        <div class="sender">${sender === 'me' ? 'Я' : 'Собеседник'}</div>
        <div class="text">${text}</div>
        <div class="time">${time}</div>
    `;

    messagesDiv.appendChild(messageDiv);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

// Завершение конференции
function endCall() {
    // Отправляем уведомление об уходе
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/leave", {}, JSON.stringify({
            type: 'leave',
            from: userId,
            to: 'all',
            data: userId + ' покинул конференцию'
        }));
    }

    // Закрываем peer connection
    if (peerConnection) {
        peerConnection.close();
    }

    // Останавливаем локальный поток
    if (localStream) {
        localStream.getTracks().forEach(track => track.stop());
    }

    // Останавливаем демонстрацию экрана
    if (screenStream) {
        screenStream.getTracks().forEach(track => track.stop());
    }

    // Отключаем WebSocket
    if (stompClient) {
        stompClient.disconnect();
    }

    // Возвращаемся к экрану подключения
    document.getElementById('conferenceScreen').style.display = 'none';
    document.getElementById('connectionScreen').style.display = 'block';

    // Очищаем видео
    document.getElementById('remoteVideo').srcObject = null;
    document.getElementById('localVideo').srcObject = null;

    // Очищаем чат
    document.getElementById('messages').innerHTML = '';

    showNotification('Конференция завершена', 'info');
}

// Обработка закрытия страницы
window.onbeforeunload = function() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/leave", {}, JSON.stringify({
            type: 'leave',
            from: userId,
            to: 'all',
            data: userId + ' покинул конференцию'
        }));
    }
};

// Инициализация при загрузке
document.addEventListener('DOMContentLoaded', function() {
    console.log('MedSoftMeet готов к работе');
});