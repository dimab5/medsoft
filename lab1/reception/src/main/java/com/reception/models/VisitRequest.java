package com.reception.models;


import java.time.LocalDateTime;
import java.util.UUID;

public record VisitRequest(
		UUID patientId,
		LocalDateTime visitTime
) {}

