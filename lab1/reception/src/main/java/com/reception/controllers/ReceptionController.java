package com.reception.controllers;

import com.reception.models.requests.CreatePatientRequest;
import com.reception.services.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RestController("/reception")
public class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping("/patients")
    public ResponseEntity<?> createPatient(@RequestBody CreatePatientRequest body) {
        receptionService.createPatient(body);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/patients/{patientId}")
    public ResponseEntity<?> deletePatient(@PathVariable UUID patientId) {
        receptionService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}
