package com.his.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.time.LocalDate;

@Value
public class PatientDto implements Patient {
    String name;
    String surname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthdate;

    public static PatientDto from(Patient patient) {
        return new PatientDto(patient.getName(), patient.getSurname(), patient.getBirthdate());
    }
}
