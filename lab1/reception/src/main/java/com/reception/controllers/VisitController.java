package com.reception.controllers;

import com.reception.models.CreateVisitRequest;
import com.reception.services.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/reception/visits")
public class VisitController {

	private final VisitService visitService;

	@PostMapping()
	public ResponseEntity<Void> registerVisit(@RequestBody CreateVisitRequest body) {
		visitService.registerVisit(body);
		return ResponseEntity.ok().build();
	}
}
