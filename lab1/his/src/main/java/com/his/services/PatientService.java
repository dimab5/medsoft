package com.his.services;

import com.his.exceptions.NotFoundException;
import com.his.models.PatientDto;
import com.his.models.PatientEntity;
import com.his.models.requests.CreatePatientRequest;
import com.his.models.requests.DeletePatientRequest;
import com.his.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientDto create(CreatePatientRequest request) {
        PatientEntity patient = PatientEntity.create(request);
        return PatientDto.from(patientRepository.save(patient));
    }

    public void delete(DeletePatientRequest request) {
        var patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> NotFoundException.patientNotFound(request.patientId()));

        patientRepository.delete(patient);
    }

    public List<PatientDto> getPatients() {
        return patientRepository.findAll().stream()
                .map(PatientDto::from)
                .toList();
    }
}
