package com.doctorApi.controllers;

import com.doctorApi.models.enums.VisitStatus;
import com.doctorApi.services.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VisitController {

	private final VisitService visitService;

	@PostMapping("/api/visits/{id}/start")
	public ResponseEntity<?> startVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.STARTED);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/api/visits/{id}/finish")
	public ResponseEntity<?> finishVisit(@PathVariable UUID id) {
		visitService.updateVisitStatus(id, VisitStatus.COMPLETED);
		return ResponseEntity.ok().build();
	}
}
