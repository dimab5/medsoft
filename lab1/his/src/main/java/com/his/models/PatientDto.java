package com.his.models;

import lombok.Value;

import java.util.Date;

@Value
public class PatientDto implements Patient {
    String name;
    String surname;
    Date birthdate;

    public static PatientDto from(Patient patient) {
        return new PatientDto(patient.getName(), patient.getSurname(), patient.getBirthdate());
    }
}
