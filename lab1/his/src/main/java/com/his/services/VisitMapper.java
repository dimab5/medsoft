package com.his.services;

import com.his.models.PatientEntity;
import com.his.models.Visit;
import com.his.models.VisitDto;
import com.his.models.enums.VisitStatus;
import com.his.models.requests.CreateVisitRequest;
import com.his.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VisitMapper {

	private final PatientRepository patientRepository;

	public Visit toEntity(CreateVisitRequest dto) {
		PatientEntity patient = patientRepository.findById(dto.patientId())
				.orElseThrow(() -> new IllegalArgumentException("Patient not found: " + dto.patientId()));

		return Visit.builder()
				.patient(patient)
				.visitTime(dto.visitTime())
				.status(VisitStatus.REGISTERED)
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
