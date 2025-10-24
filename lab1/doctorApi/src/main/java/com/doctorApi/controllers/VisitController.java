package com.doctorApi.controllers;

import com.doctorApi.models.enums.VisitStatus;
import com.doctorApi.services.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VisitController {

	private final VisitService visitService;

	@PostMapping("/api/visits/{id}/start")
	public void startVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.STARTED);
	}

	@PostMapping("/api/visits/{id}/finish")
	public void finishVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.COMPLETED);
	}
}
