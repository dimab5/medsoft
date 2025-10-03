package com.his.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HL7MessageType {
    ADMIT_DISCHARGE_TRANSFER("ADT");

    private final String code;
}