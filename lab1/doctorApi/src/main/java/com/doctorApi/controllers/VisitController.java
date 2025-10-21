package com.doctorApi.controllers;

import com.doctorApi.models.enums.VisitStatus;
import com.doctorApi.services.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

	private final VisitService visitService;

	@PostMapping("/{id}/start")
	public void startVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.IN_PROGRESS);
	}

	@PostMapping("/{id}/finish")
	public void finishVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.COMPLETED);
	}
}
