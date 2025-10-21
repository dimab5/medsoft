package com.reception.models;


import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVisitRequest(
		UUID patientId,
		LocalDateTime visitTime
) {}

