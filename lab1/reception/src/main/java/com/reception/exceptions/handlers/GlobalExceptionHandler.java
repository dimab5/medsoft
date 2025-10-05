package com.reception.exceptions.handlers;

import com.reception.exceptions.GeneralException;
import com.reception.models.ResponseError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(GeneralException.class)
    public ResponseError handlePatientAction(GeneralException ex) {
        return new ResponseError(ex.getMessage(), LocalDateTime.now());
    }
}
