package com.his.kafka;

import com.his.models.Patient;
import com.his.services.HL7ParserService;
import com.his.services.PatientService;
import com.his.websocket.PatientWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final PatientService patientService;
    private final HL7ParserService hl7ParserService;
    private final PatientWebSocketHandler webSocketHandler;

    @KafkaListener(
            topics = "reception.patient.create",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientCreate(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {
        String escapedPayload = payload
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");

        log.info("Received message: {}", escapedPayload);

        var parsed = hl7ParserService.parseCreatePatientMessage(payload);

        Patient patient = patientService.create(parsed);
        webSocketHandler.broadcastCreatePatient(patient);

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "reception.patient.delete",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientDelete(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {
        String escapedPayload = payload
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");

        log.info("Received message: {}", escapedPayload);

        var parsed = hl7ParserService.parseDeletePatientMessage(payload);

        Patient patient = patientService.getById(parsed);
        patientService.deleteById(parsed);

        webSocketHandler.broadcastDeletePatient(patient);

        acknowledgment.acknowledge();
    }
}
