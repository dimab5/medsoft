package com.his.controller;

import com.his.models.VisitDtoWithPatient;
import com.his.services.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;


    @GetMapping("/api/visits")
    public ResponseEntity<List<VisitDtoWithPatient>> finishVisit() {
        return ResponseEntity.of(Optional.of(visitService.getAllVisits()));
    }
}
