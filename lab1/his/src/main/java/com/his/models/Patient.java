package com.his.models;

import java.time.LocalDate;
import java.util.Date;

public interface Patient {
    String getName();
    String getSurname();
    LocalDate getBirthdate();
}
