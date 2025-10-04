package com.his.models;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public interface Patient {
    String getName();
    String getSurname();

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate getBirthdate();
}
