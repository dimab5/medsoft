package com.his.services.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.his.models.PatientEntity;
import com.his.models.Visit;
import com.his.models.enums.VisitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FhirParserService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public Visit parseVisit(String fhirJson) {
		try {
			JsonNode root = objectMapper.readTree(fhirJson);

			String patientRef = root.path("subject").path("reference").asText();
			String patientIdStr = patientRef.replace("Patient/", "");
			UUID patientId = UUID.fromString(patientIdStr);

			String statusStr = root.path("status").asText("registered");
			VisitStatus status = VisitStatus.valueOf(statusStr.toUpperCase());

			String startTimeStr = root.path("period").path("start").asText();
			LocalDateTime visitTime = LocalDateTime.parse(startTimeStr);

			PatientEntity patient = new PatientEntity();
			patient.setId(patientId);

			return Visit.builder()
					.patient(patient)
					.visitTime(visitTime)
					.status(status)
					.build();

		} catch (Exception e) {
			throw new RuntimeException("Error parsing FHIR message: " + e.getMessage(), e);
		}
	}
}
