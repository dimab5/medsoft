package com.reception.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdministrativeSex {
    MALE("M"),
    FEMALE("F"),
    UNKNOWN("U"),
    OTHER("O");

    private final String code;
}