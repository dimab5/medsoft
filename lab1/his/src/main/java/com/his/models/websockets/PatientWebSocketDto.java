package com.his.models.websockets;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.his.models.Patient;
import com.his.models.enums.PatientAction;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDate;

@Value
@AllArgsConstructor
public class PatientWebSocketDto implements Patient {
    String name;
    String surname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birthdate;

    PatientAction action;

    public static PatientWebSocketDto from(Patient patient, PatientAction action) {
        return new PatientWebSocketDto(
                patient.getName(),
                patient.getSurname(),
                patient.getBirthdate(),
                action
        );
    }
}
