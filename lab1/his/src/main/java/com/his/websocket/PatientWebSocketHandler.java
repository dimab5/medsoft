package com.his.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.his.models.Patient;
import com.his.models.enums.PatientAction;
import com.his.models.websockets.PatientWebSocketDto;
import com.his.services.PatientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PatientWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = Collections.synchronizedList(new ArrayList<>());

    private final ObjectMapper objectMapper;
    private final PatientService patientService;

    public PatientWebSocketHandler(PatientService patientService) {
        this.patientService = patientService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Chief UI connected via WSS: {}", session.getId());
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Chief UI disconnected: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("Received message from UI: {}", payload);

        try {
            Map<String, String> msg = objectMapper.readValue(payload, Map.class);
            String action = msg.get("action");

            if ("GET_ALL".equalsIgnoreCase(action)) {
                var patients = patientService.getPatients();
                String json = objectMapper.writeValueAsString(patients);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    public void broadcastCreatePatient(Patient patient) {
        log.info("📢 Broadcasting CREATE for {}", patient.getId());

        broadcastPatient(PatientWebSocketDto.from(patient, PatientAction.CREATE));
    }

    public void broadcastDeletePatient(Patient patient) {
        log.info("📢 Broadcasting DELETE for {}", patient.getId());

        broadcastPatient(PatientWebSocketDto.from(patient, PatientAction.DELETE));
    }

    private void broadcastMessage(String message) {
        synchronized (sessions) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        log.info("Message sent to session: {}", session.getId());
                    }
                } catch (IOException e) {
                    log.error("Error sending message to session: {}", session.getId(), e);
                }
            }
        }
    }

    private void broadcastPatient(Patient patient) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(patient);
            broadcastMessage(jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Error converting patient to JSON", e);
        }
    }
}