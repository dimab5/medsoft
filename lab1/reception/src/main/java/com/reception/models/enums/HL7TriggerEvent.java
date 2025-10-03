package com.reception.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HL7TriggerEvent {
    ADMIT("A01"),
    DISCHARGE("A03");

    private final String code;
}