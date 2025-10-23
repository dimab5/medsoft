package com.reception.services;

import com.reception.kafka.KafkaProducer;
import com.reception.models.VisitRequest;
import com.reception.services.fhir.FhirVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VisitService {

	private final FhirVisitService fhirVisitService;
	private final KafkaProducer kafkaProducer;

	public void registerVisit(VisitRequest request) {
		String fhirMessage = fhirVisitService.createVisitMessage(request);
		kafkaProducer.sendMessage("reception.visit.create", fhirMessage);
	}
}
