package com.his.services;

import com.his.models.PatientEntity;
import com.his.models.Visit;
import com.his.models.VisitDto;
import com.his.models.enums.VisitStatus;
import com.his.models.requests.VisitRequest;
import com.his.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VisitMapper {

	private final PatientRepository patientRepository;

	public Visit toEntity(VisitRequest dto) {
		PatientEntity patient = patientRepository.findById(dto.patientId())
				.orElseThrow(() -> new IllegalArgumentException("Patient not found: " + dto.patientId()));

		return Visit.builder()
				.patient(patient)
				.visitTime(dto.visitTime())
				.build();
	}

	public VisitDto toDto(Visit entity) {
		return new VisitDto(
				entity.getId(),
				entity.getPatient().getId(),
				entity.getVisitTime(),
				entity.getStatus()
		);
	}
}
