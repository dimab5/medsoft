// ─── Глобальное состояние ────────────────────────────────────────────────────
let localStream;
let peerConnection;
let stompClient;
let userId;
let remoteUserId;
let isVideoEnabled   = true;
let isAudioEnabled   = true;
let isScreenSharing  = false;
let screenStream;
let iceCandidateBuffer   = [];
let remoteDescriptionSet = false;
let connectionNotified   = false;

let selectedVideoDeviceId = null;
let selectedAudioDeviceId = null;

// ─── ICE / STUN конфигурация ─────────────────────────────────────────────────
const RTC_CONFIG = {
	iceServers: [
		{ urls: 'stun:stun.l.google.com:19302' },
		{ urls: 'stun:stun1.l.google.com:19302' },
		{ urls: 'stun:stun.cloudflare.com:3478' },
	],
	iceCandidatePoolSize: 10
};

// ─── Уведомления ─────────────────────────────────────────────────────────────
function showNotification(message, type = 'info') {
	const area = document.getElementById('notificationArea');
	const el   = document.createElement('div');
	el.className   = `notification ${type}`;
	el.textContent = message;
	area.appendChild(el);
	setTimeout(() => el.remove(), 3500);
}

// ─── Перечисление устройств ───────────────────────────────────────────────────
async function populateDevices(selectVideo, selectAudio) {
	try {
		const devices = await navigator.mediaDevices.enumerateDevices();
		const currentVideoId = selectVideo.value;
		const currentAudioId = selectAudio.value;

		selectVideo.innerHTML = '';
		selectAudio.innerHTML = '';

		devices.forEach(d => {
			const opt = document.createElement('option');
			opt.value = d.deviceId;
			opt.textContent = d.label || `Устройство (${d.deviceId.slice(0, 8)}...)`;
			if (d.kind === 'videoinput') selectVideo.appendChild(opt);
			if (d.kind === 'audioinput') selectAudio.appendChild(opt);
		});

		// Восстанавливаем выбор
		if (currentVideoId) selectVideo.value = currentVideoId;
		if (currentAudioId) selectAudio.value = currentAudioId;
	} catch (err) {
		console.warn('Не удалось получить список устройств:', err);
	}
}

async function populateLoginDevices() {
	try {
		// Запрашиваем разрешение, чтобы браузер показал метки
		const tempStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
		tempStream.getTracks().forEach(t => t.stop());

		const vs = document.getElementById('videoDeviceSelect');
		const as = document.getElementById('audioDeviceSelect');
		await populateDevices(vs, as);

		selectedVideoDeviceId = vs.value;
		selectedAudioDeviceId = as.value;

		vs.onchange = () => { selectedVideoDeviceId = vs.value; };
		as.onchange = () => { selectedAudioDeviceId = as.value; };
	} catch (err) {
		console.warn('Не удалось заполнить устройства на экране входа:', err);
	}
}

// ─── Смена устройства во время звонка ────────────────────────────────────────
async function switchVideoDevice(deviceId) {
	if (!deviceId || !localStream || !peerConnection) return;
	try {
		// Останавливаем старый видео трек
		localStream.getVideoTracks().forEach(t => t.stop());

		const newStream = await navigator.mediaDevices.getUserMedia({
			video: { deviceId: { exact: deviceId } },
			audio: false
		});
		const newVideoTrack = newStream.getVideoTracks()[0];

		// Заменяем трек в localStream
		localStream.getVideoTracks().forEach(t => localStream.removeTrack(t));
		localStream.addTrack(newVideoTrack);

		// Обновляем PeerConnection
		const sender = peerConnection.getSenders().find(s => s.track?.kind === 'video');
		if (sender && !isScreenSharing) await sender.replaceTrack(newVideoTrack);

		// Обновляем превью
		if (!isScreenSharing) {
			document.getElementById('localVideo').srcObject = localStream;
		}

		selectedVideoDeviceId = deviceId;
		showNotification('Камера переключена', 'success');
	} catch (err) {
		console.error('Ошибка переключения камеры:', err);
		showNotification('Не удалось переключить камеру', 'error');
	}
}

async function switchAudioDevice(deviceId) {
	if (!deviceId || !localStream || !peerConnection) return;
	try {
		localStream.getAudioTracks().forEach(t => t.stop());

		const newStream = await navigator.mediaDevices.getUserMedia({
			video: false,
			audio: { deviceId: { exact: deviceId } }
		});
		const newAudioTrack = newStream.getAudioTracks()[0];
		newAudioTrack.enabled = isAudioEnabled;

		localStream.getAudioTracks().forEach(t => localStream.removeTrack(t));
		localStream.addTrack(newAudioTrack);

		const sender = peerConnection.getSenders().find(s => s.track?.kind === 'audio');
		if (sender) await sender.replaceTrack(newAudioTrack);

		selectedAudioDeviceId = deviceId;
		showNotification('Микрофон переключён', 'success');
	} catch (err) {
		console.error('Ошибка переключения микрофона:', err);
		showNotification('Не удалось переключить микрофон', 'error');
	}
}

// Инициализируем выпадалки внутри звонка
async function initCallDevices() {
	const vs = document.getElementById('callVideoSelect');
	const as = document.getElementById('callAudioSelect');
	if (!vs || !as) return;

	await populateDevices(vs, as);

	if (selectedVideoDeviceId) vs.value = selectedVideoDeviceId;
	if (selectedAudioDeviceId) as.value = selectedAudioDeviceId;

	vs.onchange = () => switchVideoDevice(vs.value);
	as.onchange = () => switchAudioDevice(as.value);
}

// ─── Подключение к конференции ────────────────────────────────────────────────
async function connect() {
	userId       = document.getElementById('userId').value.trim();
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
		const constraints = {
			video: selectedVideoDeviceId ? { deviceId: { exact: selectedVideoDeviceId } } : true,
			audio: selectedAudioDeviceId ? { deviceId: { exact: selectedAudioDeviceId } } : true,
		};
		localStream = await navigator.mediaDevices.getUserMedia(constraints);

		const localVideo = document.getElementById('localVideo');
		localVideo.srcObject    = localStream;
		localVideo.style.transform = 'scaleX(-1)';

		document.getElementById('connectionScreen').style.display = 'none';
		document.getElementById('conferenceScreen').style.display  = 'block';
		document.getElementById('localId').textContent  = userId;
		document.getElementById('remoteId').textContent = remoteUserId;

		// Заполняем устройства внутри звонка
		await initCallDevices();

		connectToSignalingServer();
		showNotification('Подключение к конференции...', 'info');
	} catch (err) {
		console.error('Ошибка доступа к медиаустройствам:', err);
		showNotification('Не удалось получить доступ к камере и микрофону', 'error');
	}
}

// ─── Signaling сервер ─────────────────────────────────────────────────────────
function connectToSignalingServer() {
	const wsUrl  = window.location.origin + '/ws';
	const socket = new SockJS(wsUrl);
	stompClient  = Stomp.over(socket);
	stompClient.debug = null;

	stompClient.connect({}, onConnected, (err) => {
		console.error('Ошибка подключения к signaling серверу:', err);
		showNotification('Ошибка подключения к серверу', 'error');
		document.getElementById('connectionStatus').textContent = '🔴 Ошибка';
	});
}

function onConnected() {
	document.getElementById('connectionStatus').textContent = '🟡 Ожидание собеседника...';

	stompClient.subscribe('/topic/signal/' + userId, (msg) => {
		handleSignal(JSON.parse(msg.body));
	});

	stompClient.subscribe('/topic/join', (msg) => {
		const signal = JSON.parse(msg.body);
		if (signal.from === remoteUserId) {
			showNotification('Собеседник подключился!', 'success');
			document.getElementById('remoteStatus').textContent = '🟡 Подключается...';
			if (userId < remoteUserId) makeCall();
		}
	});

	stompClient.subscribe('/topic/leave', (msg) => {
		const signal = JSON.parse(msg.body);
		if (signal.from === remoteUserId) {
			showNotification('Собеседник покинул конференцию', 'error');
			document.getElementById('remoteVideo').srcObject = null;
			document.getElementById('remoteStatus').textContent = '❌ Собеседник отключился';
			document.getElementById('connectionStatus').textContent = '⚪ Собеседник отключился';
		}
	});

	stompClient.send('/app/join', {}, JSON.stringify({
		type: 'join', from: userId, to: 'all', data: null
	}));

	createPeerConnection();

	if (userId < remoteUserId) {
		setTimeout(() => {
			if (peerConnection &&
				peerConnection.signalingState === 'stable' &&
				!remoteDescriptionSet) {
				makeCall();
			}
		}, 1500);
	}
}

// ─── PeerConnection ───────────────────────────────────────────────────────────
function createPeerConnection() {
	peerConnection       = new RTCPeerConnection(RTC_CONFIG);
	remoteDescriptionSet = false;
	iceCandidateBuffer   = [];
	connectionNotified   = false;

	localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));

	peerConnection.onicecandidate = ({ candidate }) => {
		if (candidate) sendSignal('ice-candidate', { candidate });
	};

	peerConnection.oniceconnectionstatechange = () => {
		console.log('ICE state:', peerConnection.iceConnectionState);
	};

	peerConnection.ontrack = ({ streams }) => {
		const remoteVideo = document.getElementById('remoteVideo');
		if (streams[0] && remoteVideo.srcObject !== streams[0]) {
			remoteVideo.srcObject = streams[0];
		}
		if (!connectionNotified) {
			connectionNotified = true;
			document.getElementById('remoteStatus').textContent = '🟢 В сети';
			showNotification('Соединение установлено!', 'success');
		}
	};

	peerConnection.onconnectionstatechange = () => {
		const state  = peerConnection.connectionState;
		const status = document.getElementById('connectionStatus');
		const map = {
			connecting:   '🟡 Устанавливается...',
			connected:    '🟢 Соединение установлено',
			disconnected: '🟠 Соединение прервано',
			failed:       '🔴 Ошибка соединения',
			closed:       '⚪ Закрыто',
		};
		status.textContent = map[state] || state;
		if (state === 'disconnected' || state === 'failed') {
			document.getElementById('remoteVideo').srcObject = null;
			document.getElementById('remoteStatus').textContent = '❌ Связь потеряна';
		}
	};
}

// ─── Offer / Answer ───────────────────────────────────────────────────────────
async function makeCall() {
	if (!peerConnection || peerConnection.signalingState !== 'stable') return;
	try {
		const offer = await peerConnection.createOffer({
			offerToReceiveVideo: true,
			offerToReceiveAudio: true,
		});
		await peerConnection.setLocalDescription(offer);
		sendSignal('offer', { sdp: peerConnection.localDescription });
	} catch (err) {
		console.error('Ошибка создания offer:', err);
		showNotification('Ошибка создания соединения', 'error');
	}
}

// ─── Обработка входящих сигналов ─────────────────────────────────────────────
async function handleSignal(signal) {
	if (signal.from !== remoteUserId) return;

	try {
		switch (signal.type) {
			case 'offer': {
				if (peerConnection.signalingState !== 'stable') {
					await peerConnection.setLocalDescription({ type: 'rollback' });
				}
				await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
				remoteDescriptionSet = true;
				await flushIceCandidates();
				const answer = await peerConnection.createAnswer();
				await peerConnection.setLocalDescription(answer);
				sendSignal('answer', { sdp: peerConnection.localDescription });
				break;
			}
			case 'answer': {
				if (peerConnection.signalingState === 'have-local-offer') {
					await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
					remoteDescriptionSet = true;
					await flushIceCandidates();
				}
				break;
			}
			case 'ice-candidate': {
				if (signal.data?.candidate) {
					if (remoteDescriptionSet) {
						await peerConnection.addIceCandidate(new RTCIceCandidate(signal.data.candidate));
					} else {
						iceCandidateBuffer.push(signal.data.candidate);
					}
				}
				break;
			}
			case 'chat':
				displayMessage('remote', signal.data.text, signal.data.senderName || remoteUserId);
				break;
			case 'screen-share-start':
				showNotification('Собеседник начал демонстрацию экрана 🖥️', 'info');
				document.getElementById('remoteStatus').textContent = '🖥️ Демонстрация';
				break;
			case 'screen-share-stop':
				showNotification('Собеседник завершил демонстрацию экрана', 'info');
				document.getElementById('remoteStatus').textContent = '🟢 В сети';
				break;
		}
	} catch (err) {
		console.error('Ошибка обработки сигнала', signal.type, err);
	}
}

async function flushIceCandidates() {
	for (const candidate of iceCandidateBuffer) {
		try {
			await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
		} catch (e) {
			console.warn('Ошибка буферизированного ICE кандидата:', e);
		}
	}
	iceCandidateBuffer = [];
}

// ─── Отправка сигнала ─────────────────────────────────────────────────────────
function sendSignal(type, data) {
	if (stompClient?.connected) {
		stompClient.send('/app/signal', {}, JSON.stringify({
			type, from: userId, to: remoteUserId, data
		}));
	}
}

// ─── Управление видео ─────────────────────────────────────────────────────────
function toggleVideo() {
	isVideoEnabled = !isVideoEnabled;
	localStream?.getVideoTracks().forEach(t => t.enabled = isVideoEnabled);

	const btn = document.getElementById('toggleVideoBtn');
	const ind = document.getElementById('videoIndicator');
	if (isVideoEnabled) {
		btn.innerHTML = '<span class="ctrl-icon">📹</span><span class="ctrl-label">Видео</span>';
		btn.classList.remove('btn-control--off');
		ind.textContent = '📹'; ind.style.opacity = '1';
	} else {
		btn.innerHTML = '<span class="ctrl-icon">🚫</span><span class="ctrl-label">Видео выкл</span>';
		btn.classList.add('btn-control--off');
		ind.textContent = '🚫'; ind.style.opacity = '0.5';
	}
}

// ─── Управление аудио ─────────────────────────────────────────────────────────
function toggleAudio() {
	isAudioEnabled = !isAudioEnabled;
	localStream?.getAudioTracks().forEach(t => t.enabled = isAudioEnabled);

	const btn = document.getElementById('toggleAudioBtn');
	const ind = document.getElementById('audioIndicator');
	if (isAudioEnabled) {
		btn.innerHTML = '<span class="ctrl-icon">🎤</span><span class="ctrl-label">Микрофон</span>';
		btn.classList.remove('btn-control--off');
		ind.textContent = '🎤'; ind.style.opacity = '1';
	} else {
		btn.innerHTML = '<span class="ctrl-icon">🔇</span><span class="ctrl-label">Мик. выкл</span>';
		btn.classList.add('btn-control--off');
		ind.textContent = '🔇'; ind.style.opacity = '0.5';
	}
}

// ─── Громкость ────────────────────────────────────────────────────────────────
function setVolume(value) {
	const v = parseFloat(value);
	document.getElementById('remoteVideo').volume = v;
	document.getElementById('volumeValue').textContent = Math.round(v * 100) + '%';
}

// ─── Панель настроек во время звонка ─────────────────────────────────────────
function toggleSettings() {
	const panel = document.getElementById('settingsPanel');
	const btn   = document.getElementById('settingsBtn');
	const open  = panel.classList.toggle('settings-panel--open');
	btn.classList.toggle('ctrl-btn--active', open);
}

// ─── Демонстрация экрана ──────────────────────────────────────────────────────
async function toggleScreenShare() {
	if (!isScreenSharing) await startScreenShare();
	else await stopScreenShare();
}

async function startScreenShare() {
	try {
		screenStream = await navigator.mediaDevices.getDisplayMedia({
			video: { cursor: 'always' },
			audio: false,
		});
		const screenVideoTrack = screenStream.getVideoTracks()[0];

		const videoSender = peerConnection?.getSenders().find(s => s.track?.kind === 'video');
		if (videoSender) {
			await videoSender.replaceTrack(screenVideoTrack);
		} else {
			peerConnection?.addTrack(screenVideoTrack, screenStream);
		}

		const localVideo = document.getElementById('localVideo');
		localVideo.srcObject      = screenStream;
		localVideo.style.transform = 'none';

		screenVideoTrack.onended = () => stopScreenShare();

		isScreenSharing = true;
		document.getElementById('screenShareBtn').innerHTML =
			'<span class="ctrl-icon">⏹️</span><span class="ctrl-label">Остановить</span>';
		document.getElementById('screenShareBtn').classList.add('btn-control--active');
		document.getElementById('screenIndicator').classList.remove('tile-badge--hidden');

		sendSignal('screen-share-start', {});
		showNotification('Демонстрация экрана начата', 'success');
	} catch (err) {
		isScreenSharing = false;
		if (err.name !== 'NotAllowedError') {
			console.error('Ошибка демонстрации экрана:', err);
			showNotification('Не удалось начать демонстрацию экрана', 'error');
		}
	}
}

async function stopScreenShare() {
	if (!isScreenSharing) return;
	isScreenSharing = false;

	screenStream?.getTracks().forEach(t => t.stop());
	screenStream = null;

	const cameraTrack = localStream?.getVideoTracks()[0];
	const videoSender = peerConnection?.getSenders().find(s => s.track?.kind === 'video');
	if (videoSender && cameraTrack) await videoSender.replaceTrack(cameraTrack);

	const localVideo = document.getElementById('localVideo');
	localVideo.srcObject      = localStream;
	localVideo.style.transform = 'scaleX(-1)';

	document.getElementById('screenShareBtn').innerHTML =
		'<span class="ctrl-icon">🖥️</span><span class="ctrl-label">Экран</span>';
	document.getElementById('screenShareBtn').classList.remove('btn-control--active');
	document.getElementById('screenIndicator').classList.add('tile-badge--hidden');

	sendSignal('screen-share-stop', {});
	showNotification('Демонстрация экрана завершена', 'info');
}

// ─── Чат ──────────────────────────────────────────────────────────────────────
function sendMessage() {
	const input = document.getElementById('messageInput');
	const text  = input.value.trim();
	if (!text) return;
	displayMessage('me', text, 'Я');
	sendSignal('chat', { text, senderName: userId });
	input.value = '';
}

function displayMessage(type, text, name) {
	const container = document.getElementById('messages');
	const div       = document.createElement('div');
	div.className   = `message ${type === 'me' ? 'my-message' : 'remote-message'}`;
	const time = new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
	div.innerHTML = `
		<div class="sender">${escapeHtml(name || type)}</div>
		<div class="text">${escapeHtml(text)}</div>
		<div class="time">${time}</div>
	`;
	container.appendChild(div);
	container.scrollTop = container.scrollHeight;
}

function escapeHtml(str) {
	return String(str)
		.replace(/&/g, '&amp;').replace(/</g, '&lt;')
		.replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ─── Завершение конференции ───────────────────────────────────────────────────
function endCall() {
	if (stompClient?.connected) {
		stompClient.send('/app/leave', {}, JSON.stringify({
			type: 'leave', from: userId, to: 'all', data: null
		}));
		stompClient.disconnect();
	}

	peerConnection?.close();
	localStream?.getTracks().forEach(t => t.stop());
	screenStream?.getTracks().forEach(t => t.stop());

	peerConnection = null; localStream = null; screenStream = null;
	isScreenSharing = false; isVideoEnabled = true; isAudioEnabled = true;
	remoteDescriptionSet = false; connectionNotified = false; iceCandidateBuffer = [];

	document.getElementById('conferenceScreen').style.display = 'none';
	document.getElementById('connectionScreen').style.display = 'block';
	document.getElementById('remoteVideo').srcObject = null;
	document.getElementById('localVideo').srcObject  = null;
	document.getElementById('messages').innerHTML    = '';

	showNotification('Конференция завершена', 'info');
}

// ─── Уход при закрытии вкладки ────────────────────────────────────────────────
window.addEventListener('beforeunload', () => {
	if (stompClient?.connected) {
		stompClient.send('/app/leave', {}, JSON.stringify({
			type: 'leave', from: userId, to: 'all', data: null
		}));
	}
	localStream?.getTracks().forEach(t => t.stop());
	screenStream?.getTracks().forEach(t => t.stop());
});

// ─── Инициализация ────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
	populateLoginDevices();
	navigator.mediaDevices.addEventListener('devicechange', populateLoginDevices);

	document.getElementById('messageInput')?.addEventListener('keydown', (e) => {
		if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
	});

	// Закрывать панель настроек кликом вне неё
	document.addEventListener('click', (e) => {
		const panel = document.getElementById('settingsPanel');
		const btn   = document.getElementById('settingsBtn');
		if (panel?.classList.contains('settings-panel--open') &&
			!panel.contains(e.target) && !btn.contains(e.target)) {
			panel.classList.remove('settings-panel--open');
			btn.classList.remove('ctrl-btn--active');
		}
	});
});