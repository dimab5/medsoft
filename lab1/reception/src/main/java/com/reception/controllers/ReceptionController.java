package com.reception.controllers;

import com.reception.models.PatientDto;
import com.reception.services.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/reception")
public class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody PatientDto patient) {
        receptionService.test(patient);
        return ResponseEntity.ok().build();
    }
}
