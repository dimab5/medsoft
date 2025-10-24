package com.his.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.his.models.enums.VisitStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PatientWithStatusDto {
    UUID id;
    String name;
    String surname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthdate;

    VisitStatus status;

    public static PatientWithStatusDto from(Patient patient) {
        return new PatientWithStatusDto(patient.getId(), patient.getName(), patient.getSurname(), patient.getBirthdate(), null);
    }
}
