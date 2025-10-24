package com.his.kafka;

import com.his.models.Patient;
import com.his.models.Visit;
import com.his.models.VisitDto;
import com.his.models.requests.VisitRequest;
import com.his.services.HL7ParserService;
import com.his.services.PatientService;
import com.his.services.VisitService;
import com.his.services.fhir.FhirParserService;
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
    private final FhirParserService fhirParserService;
    private final VisitService visitService;
    private final KafkaProducer kafkaProducer;

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

    @KafkaListener(
            topics = "doctor.visit.update",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVisitStatusUpdated(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {
        VisitDto visit = fhirParserService.fromFhir(payload);
        VisitDto updatedVisit = visitService.updateStatus(visit.id(), visit.status());
        kafkaProducer.sendMessage("reception.visit.create.with.patient", fhirParserService.toFhir(updatedVisit));
        Patient patient = patientService.getById(updatedVisit.patientId());
        webSocketHandler.broadcastUpdateVisitStatus(patient);
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "reception.visit.create",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVisitCreated(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {
        log.info("Fhir received message: {}", payload);
        VisitDto visit = fhirParserService.fromFhir(payload);

        VisitDto newVisit = visitService.createVisit(new VisitRequest(visit.patientId(), visit.visitTime()));
        kafkaProducer.sendMessage("reception.visit.create.with.patient", fhirParserService.toFhir(newVisit));

        acknowledgment.acknowledge();
    }
}
