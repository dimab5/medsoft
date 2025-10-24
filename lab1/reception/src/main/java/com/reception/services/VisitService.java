package com.reception.services;

import com.reception.kafka.KafkaProducer;
import com.reception.models.VisitRequest;
import com.reception.services.fhir.FhirVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

	private final FhirVisitService fhirVisitService;
	private final KafkaProducer kafkaProducer;

	public void registerVisit(VisitRequest request) {
		String fhirMessage = fhirVisitService.createVisitMessage(request);
		log.info(fhirMessage);
		kafkaProducer.sendMessage("reception.visit.create", fhirMessage);
	}
}
