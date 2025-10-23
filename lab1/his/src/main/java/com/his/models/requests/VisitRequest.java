package com.his.models.requests;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitRequest(
		UUID patientId,
		LocalDateTime visitTime
) {}
