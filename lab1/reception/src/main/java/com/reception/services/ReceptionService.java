package com.reception.services;

import com.reception.kafka.KafkaProducer;
import com.reception.models.requests.CreatePatientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionService {

    private final String PATIENT_CREATE_TOPIC = "reception.patient.create";
    private final String PATIENT_DELETE_TOPIC = "reception.patient.delete";

    private final HL7Service hl7Service;
    private final KafkaProducer kafkaProducer;

    public void createPatient(CreatePatientRequest request) {
        String message = hl7Service.createPatientMessage(
                request.name(),
                request.surname(),
                request.birthdate()
        );

        kafkaProducer.sendMessage(PATIENT_CREATE_TOPIC, message);
    }

    public void deletePatient(UUID patientId) {
        String message = hl7Service.deletePatientMessage(patientId);

        kafkaProducer.sendMessage(PATIENT_DELETE_TOPIC, message);
    }
}
