package com.his.services.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.his.models.Visit;
import com.his.models.enums.VisitStatus;
import com.his.models.PatientEntity;
import com.his.repositories.PatientRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FhirParserService {

	private final FhirContext fhirContext = FhirContext.forR4();
	private final PatientRepository patientRepository;

	public Visit fromFhir(String fhirJson) {
		Encounter encounter = (Encounter) fhirContext.newJsonParser()
				.parseResource(fhirJson);

		UUID id = UUID.fromString(encounter.getIdElement().getIdPart());
		String patientRef = encounter.getSubject().getReference();
		UUID patientId = UUID.fromString(patientRef.replace("Patient/", ""));

		PatientEntity patient = patientRepository.findById(patientId)
				.orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

		VisitStatus status = VisitStatus.valueOf(encounter.getStatus().name());

		Date start = encounter.getPeriod().getStart();
		LocalDateTime visitTime = (start != null)
				? LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())
				: LocalDateTime.now();

		return Visit.builder()
				.id(id)
				.patient(patient)
				.visitTime(visitTime)
				.status(status)
				.build();
	}

	public String toFhir(Visit visit) {
		Encounter encounter = new Encounter();
		encounter.setId(visit.getId().toString());
		encounter.setStatus(Encounter.EncounterStatus.valueOf(visit.getStatus().name()));
		encounter.setSubject(new Reference("Patient/" + visit.getPatient().getId()));

		Period period = new Period();
		period.setStart(Date.from(visit.getVisitTime().atZone(ZoneId.systemDefault()).toInstant()));
		encounter.setPeriod(period);

		return fhirContext.newJsonParser()
				.setPrettyPrint(false)
				.encodeResourceToString(encounter);
	}
}
