package com.his.kafka;

import com.his.services.HL7ParserService;
import com.his.services.PatientService;
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

    @KafkaListener(
            topics = "reception.patient.create",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientCreate(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {
        var parsed = hl7ParserService.parseCreatePatientMessage(payload);
        log.info("Received message: {}", parsed);

        patientService.create(parsed);

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
        var parsed = hl7ParserService.parseDeletePatientMessage(payload);
        log.info("Received message: {}", parsed);
        acknowledgment.acknowledge();
    }
}
