package com.doctorApi.websocket;

import com.doctorApi.models.VisitDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorWebSocketHandler extends TextWebSocketHandler {

	private final ObjectMapper mapper = new ObjectMapper();
	private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.add(session);
		log.info("Doctor UI connected: {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
		sessions.remove(session);
		log.info("Doctor UI disconnected: {}", session.getId());
	}

	public void broadcastVisitUpdate(VisitDto visit) {
		try {
			String message = mapper.writeValueAsString(visit);
			for (WebSocketSession session : sessions) {
				session.sendMessage(new TextMessage(message));
			}
		} catch (Exception e) {
			log.error("WebSocket broadcast error", e);
		}
	}
}

