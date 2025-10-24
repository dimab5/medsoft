package com.his.services;

import com.his.exceptions.NotFoundException;
import com.his.models.PatientDto;
import com.his.models.PatientEntity;
import com.his.models.PatientWithStatusDto;
import com.his.models.Visit;
import com.his.models.requests.CreatePatientRequest;
import com.his.repositories.PatientRepository;
import com.his.repositories.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

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

    public List<PatientWithStatusDto> getPatients() {
        return patientRepository.findAll().stream()
                .map(patient -> {
                    Visit visit = visitRepository.findFirstByPatientIdOrderByVisitTimeDesc(patient.getId())
                            .orElse(null);

                    PatientWithStatusDto patientWithStatusDto = PatientWithStatusDto.from(patient);
                    if (visit != null) {
                        patientWithStatusDto.setStatus(visit.getStatus());
                    }

                    return patientWithStatusDto;
                })
                .toList();
    }
}
