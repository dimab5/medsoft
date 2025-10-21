package com.his.models;

import com.his.models.enums.VisitStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDto(
		UUID id,
		UUID patientId,
		LocalDateTime visitTime,
		VisitStatus status
) {}
