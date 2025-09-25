package com.reception.models;

import java.time.LocalDate;

public record PatientDto(
        String name,
        String surname,
        LocalDate birthDate
) {}
