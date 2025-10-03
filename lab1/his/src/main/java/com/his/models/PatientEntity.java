package com.his.models;

import com.his.models.requests.CreatePatientRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "patients")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientEntity implements Patient {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "birthdate", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date birthdate;

    public static PatientEntity create(CreatePatientRequest request) {
        PatientEntity patient = new PatientEntity();
        patient.setName(request.name());
        patient.setSurname(request.surname());
        patient.setBirthdate(request.birthdate());
        return patient;
    }
}
