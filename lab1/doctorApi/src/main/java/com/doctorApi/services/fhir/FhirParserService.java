package com.doctorApi.services.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.doctorApi.models.VisitDtoWithPatient;
import org.hl7.fhir.r4.model.*;
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

	private Encounter.EncounterStatus toFhirStatus(VisitStatus status) {
		switch (status) {
			case PLANNED:
				return Encounter.EncounterStatus.PLANNED;
			case STARTED:
				return Encounter.EncounterStatus.INPROGRESS;
			case COMPLETED:
				return Encounter.EncounterStatus.FINISHED;
			case CANCELLED:
				return Encounter.EncounterStatus.CANCELLED;
			default:
				return Encounter.EncounterStatus.UNKNOWN;
		}
	}

	private VisitStatus fromFhirStatus(Encounter.EncounterStatus fhirStatus) {
		switch (fhirStatus) {
			case PLANNED:
				return VisitStatus.PLANNED;
			case INPROGRESS:
				return VisitStatus.STARTED;
			case FINISHED:
				return VisitStatus.COMPLETED;
			case CANCELLED:
				return VisitStatus.CANCELLED;
			default:
				return VisitStatus.PLANNED;
		}
	}

	public VisitDtoWithPatient fromFhirJson(String fhirJson) {
		Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(fhirJson);

		Encounter encounter = null;
		Patient patient = null;

		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Encounter enc) {
				encounter = enc;
			} else if (entry.getResource() instanceof Patient pat) {
				patient = pat;
			}
		}

		if (encounter == null) {
			throw new IllegalArgumentException("No Encounter found in FHIR message");
		}
		if (patient == null) {
			throw new IllegalArgumentException("No Patient found in FHIR message");
		}

		UUID id = UUID.fromString(encounter.getIdElement().getIdPart());
		String patientRef = encounter.getSubject().getReference();
		UUID patientId = UUID.fromString(patientRef.replace("Patient/", ""));

		VisitStatus status = fromFhirStatus(encounter.getStatus());

		Date start = encounter.getPeriod().getStart();
		LocalDateTime visitTime = (start != null)
				? LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())
				: LocalDateTime.now();

		String name = "";
		String surname = "";

		if (!patient.getName().isEmpty()) {
			HumanName humanName = patient.getName().get(0);
			if (!humanName.getGiven().isEmpty()) {
				name = humanName.getGiven().get(0).getValueNotNull();
			}
			if (humanName.hasFamily()) {
				surname = humanName.getFamily();
			}
		}

		return new VisitDtoWithPatient(id, patientId, visitTime, status, name, surname);
	}

	public String toFhirJson(VisitDto dto) {
		Encounter encounter = new Encounter();
		encounter.setId(dto.getId().toString());

		encounter.setStatus(toFhirStatus(dto.getStatus()));

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