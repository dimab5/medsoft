package com.doctorApi.models;

import com.doctorApi.models.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDtoWithPatient(
        UUID id,
        UUID patientId,
        LocalDateTime visitTime,
        VisitStatus status,
        String name,
        String surname
) {
}
