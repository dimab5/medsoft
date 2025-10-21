package com.his.services;

import com.his.models.Visit;
import com.his.models.VisitDto;
import com.his.models.enums.VisitStatus;
import com.his.models.requests.CreateVisitRequest;
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

	public VisitDto createVisit(CreateVisitRequest dto) {
		Visit visit = visitMapper.toEntity(dto);
		visit = visitRepository.save(visit);
		return visitMapper.toDto(visit);
	}

	public List<VisitDto> getAllVisits() {
		return visitRepository.findAll()
				.stream()
				.map(visitMapper::toDto)
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

