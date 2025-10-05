package com.his.services;

import com.his.exceptions.NotFoundException;
import com.his.models.PatientDto;
import com.his.models.PatientEntity;
import com.his.models.requests.CreatePatientRequest;
import com.his.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientDto create(CreatePatientRequest request) {
        PatientEntity patient = PatientEntity.create(request);
        return PatientDto.from(patientRepository.save(patient));
    }

    public void deleteById(UUID patientId) {
        var patient = patientRepository.findById(patientId)
                .orElseThrow(() -> NotFoundException.patientNotFound(patientId));

        patientRepository.delete(patient);
    }

    public PatientDto getById(UUID patientId) {
        return PatientDto.from(patientRepository.findById(patientId)
                .orElseThrow(() -> NotFoundException.patientNotFound(patientId)));
    }

    public List<PatientDto> getPatients() {
        return patientRepository.findAll().stream()
                .map(PatientDto::from)
                .toList();
    }
}
