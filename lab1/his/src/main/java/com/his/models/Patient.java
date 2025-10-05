package com.his.models;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

public interface Patient {
    UUID getId();
    String getName();
    String getSurname();

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate getBirthdate();
}
