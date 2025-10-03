package com.reception.exceptions;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public abstract class GeneralException extends RuntimeException {
    protected String message;
    protected HttpStatus status;
}
