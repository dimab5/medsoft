package com.reception.controllers;

import com.reception.models.requests.CreatePatientRequest;
import com.reception.services.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping("/reception/patients")
    public ResponseEntity<?> createPatient(@RequestBody CreatePatientRequest body) {
        receptionService.createPatient(body);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reception/patients/{patientId}")
    public ResponseEntity<?> deletePatient(@PathVariable UUID patientId) {
        receptionService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}
