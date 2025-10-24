package com.his.services;

import com.his.models.Patient;
import com.his.models.Visit;
import com.his.models.VisitDto;
import com.his.models.VisitDtoWithPatient;
import com.his.models.enums.VisitStatus;
import com.his.models.requests.VisitRequest;
import com.his.repositories.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

	private final VisitRepository visitRepository;
	private final VisitMapper visitMapper;
	private final PatientService patientService;

	public VisitDto createVisit(VisitRequest dto) {
		Visit visit = visitMapper.toEntity(dto);
		visit.setStatus(VisitStatus.PLANNED);
		visit = visitRepository.save(visit);
		return visitMapper.toDto(visit);
	}

	public List<VisitDtoWithPatient> getAllVisits() {
		return visitRepository.findAll()
				.stream()
				.map(visit -> {
					VisitDtoWithPatient visitAns = VisitDtoWithPatient.from(visit);
					Patient patient = patientService.getById(visit.getPatient().getId());
					return visitAns.withName(patient.getName()).withSurname(patient.getSurname());
				})
				.toList();
	}

	public VisitDto updateStatus(UUID visitId, VisitStatus newStatus) {
		Visit visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new IllegalArgumentException("Visit not found: " + visitId));

		visit.setStatus(newStatus);
		visitRepository.save(visit);

		return visitMapper.toDto(visit);
	}

	public void deleteVisit(UUID visitId) {
		visitRepository.deleteById(visitId);
	}
}

