package com.his.models;

import lombok.Value;

import java.time.LocalDate;

@Value
public class PatientDto implements Patient {
    String name;
    String surname;
    LocalDate birthdate;

    public static PatientDto from(Patient patient) {
        return new PatientDto(patient.getName(), patient.getSurname(), patient.getBirthdate());
    }
}
