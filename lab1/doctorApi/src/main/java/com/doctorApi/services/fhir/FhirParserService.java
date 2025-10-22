package com.doctorApi.services.fhir;

import com.doctorApi.models.VisitDto;
import com.doctorApi.models.enums.VisitStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FhirParserService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public VisitDto fromFhirJson(String fhirJson) {
		try {
			JsonNode root = objectMapper.readTree(fhirJson);

			VisitDto visit = new VisitDto();
			visit.setId(UUID.fromString(root.path("id").asText(UUID.randomUUID().toString())));

			String patientRef = root.path("subject").path("reference").asText();
			String patientIdStr = patientRef.replace("Patient/", "");
			visit.setPatientId(UUID.fromString(patientIdStr));

			visit.setStatus(VisitStatus.valueOf(root.path("status").asText("registered").toUpperCase()));

			String startTimeStr = root.path("period").path("start").asText();
			visit.setVisitTime(LocalDateTime.parse(startTimeStr));

			return visit;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing FHIR JSON: " + e.getMessage(), e);
		}
	}

	public String toFhirJson(VisitDto visit) {
		try {
			String patientRef = "Patient/" + visit.getPatientId();
			return """
                {
                  "resourceType": "Encounter",
                  "id": "%s",
                  "status": "%s",
                  "subject": { "reference": "%s" },
                  "period": { "start": "%s" }
                }
            """.formatted(
					visit.getId(),
					visit.getStatus().name().toLowerCase(),
					patientRef,
					visit.getVisitTime().toString()
			);
		} catch (Exception e) {
			throw new RuntimeException("Error creating FHIR JSON: " + e.getMessage(), e);
		}
	}
}
