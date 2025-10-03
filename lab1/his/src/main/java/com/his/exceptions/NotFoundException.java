package com.his.exceptions;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NotFoundException extends GeneralException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public static NotFoundException patientNotFound(UUID patientId) {
        return new NotFoundException("Patient with id %s not found".formatted(patientId));
    }
}
