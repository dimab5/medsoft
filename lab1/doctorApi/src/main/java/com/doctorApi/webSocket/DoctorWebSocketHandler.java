package com.doctorApi.websocket;

import com.doctorApi.models.VisitDto;
import com.doctorApi.models.VisitDtoWithPatient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

	public void broadcastVisitUpdate(VisitDtoWithPatient visit) {
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

