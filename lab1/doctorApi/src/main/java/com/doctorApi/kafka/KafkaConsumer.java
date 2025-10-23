package com.doctorApi.kafka;

import com.doctorApi.services.fhir.FhirParserService;
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

    private final FhirParserService fhirParserService;

    @KafkaListener(
            topics = "reception.visit.create",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePatientCreate(
            @Payload String payload,
            Acknowledgment acknowledgment
    ) {

    }
}
