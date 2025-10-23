package com.doctorApi.services;

import com.doctorApi.kafka.KafkaProducer;
import com.doctorApi.models.VisitDto;
import com.doctorApi.models.enums.VisitStatus;
import com.doctorApi.services.fhir.FhirParserService;
import com.doctorApi.websocket.DoctorWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

	private final FhirParserService fhirParserService;
	private final KafkaProducer kafkaProducer;
	private final DoctorWebSocketHandler webSocketHandler;

	public void updateVisitStatus(UUID visitId, VisitStatus status) {
		VisitDto visit = new VisitDto();
		visit.setId(visitId);
		visit.setStatus(status);

		String fhirJson = fhirParserService.toFhirJson(visit);
		kafkaProducer.sendMessage("doctor.visit.update", fhirJson);

		log.info("Sent visit {} status {} to HIS", visitId, status);
	}

	public void handleVisitFromHis(String fhirJson) {
		VisitDto visit = fhirParserService.fromFhirJson(fhirJson);
		webSocketHandler.broadcastVisitUpdate(visit);
		log.info("Broadcasted visit {} to UI", visit.getId());
	}
}


