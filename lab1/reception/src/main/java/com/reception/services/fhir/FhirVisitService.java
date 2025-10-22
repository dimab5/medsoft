package com.reception.services.fhir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reception.models.CreateVisitRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FhirVisitService {

	private final ObjectMapper mapper = new ObjectMapper();

	public String createVisitMessage(CreateVisitRequest request) {
		try {
			Map<String, Object> fhir = new HashMap<>();
			fhir.put("resourceType", "Encounter");
			fhir.put("status", "registered");

			Map<String, Object> subject = new HashMap<>();
			subject.put("reference", "Patient/" + request.patientId());
			fhir.put("subject", subject);

			Map<String, Object> period = new HashMap<>();
			period.put("start", request.visitTime().toString());
			fhir.put("period", period);

			return mapper.writeValueAsString(fhir);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize FHIR message", e);
		}
	}
}
