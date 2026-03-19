let localStream;
let peerConnection;
let stompClient;
let userId;
let roomId;
let remoteUserId = null;
let isVideoEnabled = true;
let isAudioEnabled = true;
let isScreenSharing = false;
let screenStream;
let iceCandidateBuffer = [];
let remoteDescriptionSet = false;
let connectionNotified = false;
let isOfferInProgress = false;
let connectionTimeout = null;

let selectedVideoDeviceId = null;
let selectedAudioDeviceId = null;

const RTC_CONFIG = {
	iceServers: [
		{ urls: 'stun:stun.l.google.com:19302' },
		{ urls: 'stun:stun1.l.google.com:19302' },
		{ urls: 'stun:stun.cloudflare.com:3478' },
	],
	iceCandidatePoolSize: 10
};

function showNotification(message, type = 'info') {
	const area = document.getElementById('notificationArea');
	const el = document.createElement('div');
	el.className = `notification ${type}`;
	el.textContent = message;
	area.appendChild(el);
	setTimeout(() => el.remove(), 3500);
}

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

		if (currentVideoId) selectVideo.value = currentVideoId;
		if (currentAudioId) selectAudio.value = currentAudioId;
	} catch (err) {
		console.warn('Не удалось получить список устройств:', err);
	}
}

async function populateLoginDevices() {
	try {
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

async function switchVideoDevice(deviceId) {
	if (!deviceId || !localStream || !peerConnection) return;
	try {
		localStream.getVideoTracks().forEach(t => t.stop());

		const newStream = await navigator.mediaDevices.getUserMedia({
			video: { deviceId: { exact: deviceId } },
			audio: false
		});
		const newVideoTrack = newStream.getVideoTracks()[0];

		localStream.getVideoTracks().forEach(t => localStream.removeTrack(t));
		localStream.addTrack(newVideoTrack);

		const sender = peerConnection.getSenders().find(s => s.track?.kind === 'video');
		if (sender && !isScreenSharing) await sender.replaceTrack(newVideoTrack);

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

async function connect() {
	userId = document.getElementById('userId').value.trim();
	roomId = document.getElementById('roomId').value.trim();

	if (!userId || !roomId) {
		showNotification('Пожалуйста, введите ваше имя и ID комнаты', 'error');
		return;
	}

	try {
		const constraints = {
			video: selectedVideoDeviceId ? { deviceId: { exact: selectedVideoDeviceId } } : true,
			audio: selectedAudioDeviceId ? { deviceId: { exact: selectedAudioDeviceId } } : true,
		};
		localStream = await navigator.mediaDevices.getUserMedia(constraints);

		const localVideo = document.getElementById('localVideo');
		localVideo.srcObject = localStream;
		localVideo.style.transform = 'scaleX(-1)';

		document.getElementById('connectionScreen').style.display = 'none';
		document.getElementById('conferenceScreen').style.display = 'block';
		document.getElementById('localId').textContent = userId;
		document.getElementById('roomDisplay').textContent = roomId;

		await initCallDevices();

		connectToSignalingServer();
		showNotification('Подключение к комнате ' + roomId + '...', 'info');
	} catch (err) {
		console.error('Ошибка доступа к медиаустройствам:', err);
		showNotification('Не удалось получить доступ к камере и микрофону', 'error');
	}
}

function connectToSignalingServer() {
	const wsUrl = window.location.origin + '/ws';
	const socket = new SockJS(wsUrl);
	stompClient = Stomp.over(socket);
	stompClient.debug = null;

	stompClient.connect({}, onConnected, (err) => {
		console.error('Ошибка подключения к signaling серверу:', err);
		showNotification('Ошибка подключения к серверу', 'error');
		document.getElementById('connectionStatus').textContent = '🔴 Ошибка';
	});
}

function onConnected() {
	document.getElementById('connectionStatus').textContent = '🟡 Ожидание собеседника...';

	if (connectionTimeout) clearTimeout(connectionTimeout);

	connectionTimeout = setTimeout(() => {
		if (!remoteUserId) {
			showNotification('Собеседник ещё не подключился. Ожидаем...', 'info');
		}
	}, 30000);

	stompClient.subscribe('/topic/signal/' + userId, (msg) => {
		handleSignal(JSON.parse(msg.body));
	});

	stompClient.subscribe('/topic/room/' + roomId, (msg) => {
		handleRoomEvent(JSON.parse(msg.body));
	});

	stompClient.send('/app/join', {}, JSON.stringify({
		type: 'join',
		from: userId,
		to: roomId,
		data: null
	}));
}

function handleRoomEvent(signal) {
	if (signal.from === userId) return;

	if (signal.type === 'join') {
		remoteUserId = signal.from;
		showNotification(remoteUserId + ' подключился!', 'success');
		document.getElementById('remoteId').textContent = remoteUserId;
		document.getElementById('remoteStatus').textContent = '🟡 Подключается...';

		if (!peerConnection) createPeerConnection();

		if (userId < remoteUserId && !isOfferInProgress) {
			console.log(`[${userId}] Запуск вызова как инициатор`);
			setTimeout(() => makeCall(), 1000);
		} else if (userId > remoteUserId) {
			sendSignalTo(remoteUserId, 'ready', {});
		}
	} else if (signal.type === 'leave') {
		if (signal.from === remoteUserId) {
			showNotification(remoteUserId + ' покинул комнату', 'error');
			document.getElementById('remoteVideo').srcObject = null;
			document.getElementById('remoteStatus').textContent = '❌ Собеседник отключился';
			document.getElementById('connectionStatus').textContent = '⚪ Собеседник отключился';
			remoteUserId = null;
			resetPeerConnection();
		}
	}
}

function resetPeerConnection() {
	if (peerConnection) {
		peerConnection.close();
		peerConnection = null;
	}
	remoteDescriptionSet = false;
	iceCandidateBuffer = [];
	connectionNotified = false;
	isOfferInProgress = false;
}

function createPeerConnection() {
	if (peerConnection) peerConnection.close();

	peerConnection = new RTCPeerConnection(RTC_CONFIG);
	remoteDescriptionSet = false;
	iceCandidateBuffer = [];
	connectionNotified = false;
	isOfferInProgress = false;

	console.log(`[${userId}] Создан peer connection с ${remoteUserId}`);

	localStream.getTracks().forEach(track => {
		peerConnection.addTrack(track, localStream);
	});

	peerConnection.onicecandidate = ({ candidate }) => {
		if (candidate && remoteUserId) {
			sendSignalTo(remoteUserId, 'ice-candidate', { candidate });
		}
	};

	peerConnection.oniceconnectionstatechange = () => {
		console.log(`[${userId}] ICE state:`, peerConnection.iceConnectionState);
	};

	peerConnection.onsignalingstatechange = () => {
		console.log(`[${userId}] Signaling state:`, peerConnection.signalingState);
		// Сбрасываем флаг когда состояние стабилизировалось
		if (peerConnection.signalingState === 'stable') {
			isOfferInProgress = false;
		}
	};

	peerConnection.onnegotiationneeded = async () => {
		if (isOfferInProgress) return;
		if (peerConnection.signalingState !== 'stable') return;
		if (!remoteUserId || userId > remoteUserId) return;

		console.log(`[${userId}] onnegotiationneeded → makeCall`);
		await makeCall();
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
		const state = peerConnection.connectionState;
		const map = {
			'connecting': '🟡 Устанавливается...',
			'connected': '🟢 Соединение установлено',
			'disconnected': '🟠 Соединение прервано',
			'failed': '🔴 Ошибка соединения',
			'closed': '⚪ Закрыто',
		};
		document.getElementById('connectionStatus').textContent = map[state] || state;

		if (state === 'connected' && connectionTimeout) {
			clearTimeout(connectionTimeout);
			connectionTimeout = null;
		}

		if (state === 'disconnected' || state === 'failed') {
			document.getElementById('remoteVideo').srcObject = null;
			document.getElementById('remoteStatus').textContent = '❌ Связь потеряна';
		}
	};
}

async function makeCall() {
	if (isOfferInProgress) {
		console.log(`[${userId}] Offer уже в процессе, пропускаем`);
		return;
	}
	if (!peerConnection) {
		console.error('[makeCall] peerConnection не существует');
		return;
	}
	if (!remoteUserId || userId > remoteUserId) {
		console.log(`[${userId}] Не инициатор, пропускаем offer`);
		return;
	}
	if (peerConnection.signalingState !== 'stable') {
		console.log(`[${userId}] Состояние не stable:`, peerConnection.signalingState);
		return;
	}

	try {
		isOfferInProgress = true;
		console.log(`[${userId}] Создание offer для ${remoteUserId}...`);

		const offer = await peerConnection.createOffer({
			offerToReceiveVideo: true,
			offerToReceiveAudio: true,
		});
		await peerConnection.setLocalDescription(offer);
		sendSignalTo(remoteUserId, 'offer', { sdp: peerConnection.localDescription });
	} catch (err) {
		console.error(`[${userId}] Ошибка создания offer:`, err);
		isOfferInProgress = false;
		showNotification('Ошибка создания соединения', 'error');
	}
}

async function handleSignal(signal) {
	if (!remoteUserId) {
		remoteUserId = signal.from;
		document.getElementById('remoteId').textContent = remoteUserId;
		if (!peerConnection) createPeerConnection();
	}

	if (signal.from !== remoteUserId) return;

	console.log(`[${userId}] Получен сигнал:`, signal.type, 'от', signal.from);

	try {
		switch (signal.type) {
			case 'offer': {
				isOfferInProgress = false;

				if (!peerConnection) createPeerConnection();

				if (peerConnection.signalingState === 'have-local-offer') {
					await peerConnection.setLocalDescription({ type: 'rollback' });
				}

				await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
				remoteDescriptionSet = true;
				await flushIceCandidates();

				const answer = await peerConnection.createAnswer();
				await peerConnection.setLocalDescription(answer);
				sendSignalTo(remoteUserId, 'answer', { sdp: peerConnection.localDescription });
				break;
			}

			case 'answer': {
				isOfferInProgress = false;
				if (peerConnection.signalingState === 'have-local-offer') {
					await peerConnection.setRemoteDescription(new RTCSessionDescription(signal.data.sdp));
					remoteDescriptionSet = true;
					await flushIceCandidates();
				}
				break;
			}

			case 'ice-candidate': {
				if (signal.data?.candidate) {
					if (remoteDescriptionSet && peerConnection) {
						try {
							await peerConnection.addIceCandidate(new RTCIceCandidate(signal.data.candidate));
						} catch (e) {
							console.warn('Ошибка добавления ICE кандидата:', e);
						}
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

			case 'ready': {
				console.log(`[${userId}] Получен ready от ${signal.from}`);
				if (!peerConnection) createPeerConnection();

				if (userId < remoteUserId && !isOfferInProgress) {
					setTimeout(() => {
						if (peerConnection?.signalingState === 'stable') makeCall();
					}, 500);
				}
				break;
			}
		}
	} catch (err) {
		console.error(`[${userId}] Ошибка обработки сигнала ${signal.type}:`, err);
	}
}

async function flushIceCandidates() {
	const buffer = [...iceCandidateBuffer];
	iceCandidateBuffer = [];
	for (const candidate of buffer) {
		try {
			await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
		} catch (e) {
			console.warn('Ошибка добавления буферизированного кандидата:', e);
		}
	}
}

function sendSignalTo(targetUserId, type, data) {
	if (!stompClient?.connected) return;
	console.log(`[${userId}] Отправка сигнала ${type} → ${targetUserId}`);
	stompClient.send('/app/signal', {}, JSON.stringify({
		type, from: userId, to: targetUserId, data
	}));
}

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

function setVolume(value) {
	const v = parseFloat(value);
	document.getElementById('remoteVideo').volume = v;
	document.getElementById('volumeValue').textContent = Math.round(v * 100) + '%';
}

function toggleSettings() {
	const panel = document.getElementById('settingsPanel');
	const btn = document.getElementById('settingsBtn');
	const open = panel.classList.toggle('settings-panel--open');
	btn.classList.toggle('ctrl-btn--active', open);
}

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
		localVideo.srcObject = screenStream;
		localVideo.style.transform = 'none';

		screenVideoTrack.onended = () => stopScreenShare();

		isScreenSharing = true;
		document.getElementById('screenShareBtn').innerHTML =
			'<span class="ctrl-icon">⏹️</span><span class="ctrl-label">Остановить</span>';
		document.getElementById('screenShareBtn').classList.add('btn-control--active');
		document.getElementById('screenIndicator').classList.remove('tile-badge--hidden');

		if (remoteUserId) sendSignalTo(remoteUserId, 'screen-share-start', {});
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
	localVideo.srcObject = localStream;
	localVideo.style.transform = 'scaleX(-1)';

	document.getElementById('screenShareBtn').innerHTML =
		'<span class="ctrl-icon">🖥️</span><span class="ctrl-label">Экран</span>';
	document.getElementById('screenShareBtn').classList.remove('btn-control--active');
	document.getElementById('screenIndicator').classList.add('tile-badge--hidden');

	if (remoteUserId) sendSignalTo(remoteUserId, 'screen-share-stop', {});
	showNotification('Демонстрация экрана завершена', 'info');
}

function sendMessage() {
	const input = document.getElementById('messageInput');
	const text = input.value.trim();
	if (!text) return;
	displayMessage('me', text, 'Я');
	if (remoteUserId) sendSignalTo(remoteUserId, 'chat', { text, senderName: userId });
	input.value = '';
}

function displayMessage(type, text, name) {
	const container = document.getElementById('messages');
	const div = document.createElement('div');
	div.className = `message ${type === 'me' ? 'my-message' : 'remote-message'}`;
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
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;');
}

function endCall() {
	if (stompClient?.connected) {
		stompClient.send('/app/leave', {}, JSON.stringify({
			type: 'leave', from: userId, to: roomId, data: null
		}));
		stompClient.disconnect();
	}

	peerConnection?.close();
	localStream?.getTracks().forEach(t => t.stop());
	screenStream?.getTracks().forEach(t => t.stop());

	peerConnection = null;
	localStream = null;
	screenStream = null;
	remoteUserId = null;
	isScreenSharing = false;
	isVideoEnabled = true;
	isAudioEnabled = true;
	remoteDescriptionSet = false;
	connectionNotified = false;
	iceCandidateBuffer = [];
	isOfferInProgress = false;

	if (connectionTimeout) {
		clearTimeout(connectionTimeout);
		connectionTimeout = null;
	}

	document.getElementById('conferenceScreen').style.display = 'none';
	document.getElementById('connectionScreen').style.display = 'block';
	document.getElementById('remoteVideo').srcObject = null;
	document.getElementById('localVideo').srcObject = null;
	document.getElementById('messages').innerHTML = '';
	document.getElementById('remoteId').textContent = '—';

	showNotification('Конференция завершена', 'info');
}

window.addEventListener('beforeunload', () => {
	if (stompClient?.connected) {
		stompClient.send('/app/leave', {}, JSON.stringify({
			type: 'leave', from: userId, to: roomId, data: null
		}));
	}
	localStream?.getTracks().forEach(t => t.stop());
	screenStream?.getTracks().forEach(t => t.stop());
});

document.addEventListener('DOMContentLoaded', () => {
	populateLoginDevices();
	navigator.mediaDevices.addEventListener('devicechange', populateLoginDevices);

	document.getElementById('messageInput')?.addEventListener('keydown', (e) => {
		if (e.key === 'Enter' && !e.shiftKey) {
			e.preventDefault();
			sendMessage();
		}
	});

	document.addEventListener('click', (e) => {
		const panel = document.getElementById('settingsPanel');
		const btn = document.getElementById('settingsBtn');
		if (panel?.classList.contains('settings-panel--open') &&
			!panel.contains(e.target) && !btn.contains(e.target)) {
			panel.classList.remove('settings-panel--open');
			btn.classList.remove('ctrl-btn--active');
		}
	});
});