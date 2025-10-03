package com.his.models.requests;

import java.time.LocalDate;

public record CreatePatientRequest(
        String name,
        String surname,
        LocalDate birthdate
) {}
