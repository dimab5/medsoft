package com.reception.services;

import com.reception.models.PatientDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionService {

    private final HL7Service hl7Service;

    public void test(PatientDto patientDto) {
        String message = hl7Service.createPatientMessage(patientDto.name(), patientDto.surname(), patientDto.birthDate());
        log.info("Message: {}", message);
    }
}
