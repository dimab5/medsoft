package com.doctorApi.models;


import com.doctorApi.models.enums.VisitStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VisitDto {
	private UUID id;
	private UUID patientId;
	private LocalDateTime visitTime;
	private VisitStatus status;
}
