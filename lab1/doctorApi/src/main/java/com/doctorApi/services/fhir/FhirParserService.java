package com.doctorApi.services.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

import com.doctorApi.models.VisitDto;
import com.doctorApi.models.enums.VisitStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class FhirParserService {

	private final FhirContext fhirContext = FhirContext.forR4();

	public VisitDto fromFhirJson(String fhirJson) {
		Encounter encounter = (Encounter) fhirContext.newJsonParser()
				.parseResource(fhirJson);

		VisitDto dto = new VisitDto();
		dto.setId(UUID.fromString(encounter.getIdElement().getIdPart()));
		dto.setPatientId(UUID.fromString(
				encounter.getSubject().getReference().replace("Patient/", "")));

		dto.setStatus(VisitStatus.valueOf(encounter.getStatus().name()));

		Date start = encounter.getPeriod().getStart();
		if (start != null) {
			dto.setVisitTime(LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault()));
		}

		return dto;
	}

	public String toFhirJson(VisitDto dto) {
		Encounter encounter = new Encounter();
		encounter.setId(dto.getId().toString());
		encounter.setStatus(Encounter.EncounterStatus.valueOf(dto.getStatus().name()));

		encounter.setSubject(new Reference("Patient/" + dto.getPatientId()));

		Period period = new Period();
		if (dto.getVisitTime() != null) {
			period.setStart(Date.from(dto.getVisitTime().atZone(ZoneId.systemDefault()).toInstant()));
		}
		encounter.setPeriod(period);

		return fhirContext.newJsonParser()
				.setPrettyPrint(false)
				.encodeResourceToString(encounter);
	}
}
