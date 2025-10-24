package com.his.services.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.his.models.PatientDto;
import com.his.models.VisitDto;
import com.his.services.PatientService;
import org.hl7.fhir.r4.model.*;
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
	private final PatientService patientService;

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

	private Encounter.EncounterStatus toFhirStatus(VisitStatus visitStatus) {
		switch (visitStatus) {
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

	public VisitDto fromFhir(String fhirJson) {
		Encounter encounter = (Encounter) fhirContext.newJsonParser()
				.parseResource(fhirJson);

		UUID id = null;
		if (encounter.hasIdElement() && encounter.getIdElement().hasIdPart()) {
			try {
				id = UUID.fromString(encounter.getIdElement().getIdPart());
			} catch (IllegalArgumentException e) {
				id = null;
			}
		}

		UUID patientId = null;
		PatientDto patient = null;

		if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
			String patientRef = encounter.getSubject().getReference();
			if (patientRef != null && patientRef.startsWith("Patient/")) {
				try {
					String patientIdStr = patientRef.replace("Patient/", "");
					patientId = UUID.fromString(patientIdStr);
					patient = patientService.getById(patientId);
				} catch (IllegalArgumentException e) {
					patientId = null;
				} catch (Exception e) {
					patientId = null;
				}
			}
		}

		UUID finalPatientId = (patient != null) ? patient.getId() : patientId;

		VisitStatus status = fromFhirStatus(encounter.getStatus());

		LocalDateTime visitTime = null;
		if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
			Date start = encounter.getPeriod().getStart();
			if (start != null) {
				visitTime = LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
			}
		}

		return new VisitDto(id, finalPatientId, visitTime, status);
	}

	public String toFhir(VisitDto visit) {
		PatientDto patient = patientService.getById(visit.patientId());

		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.MESSAGE);
		bundle.setId(UUID.randomUUID().toString());

		Patient patientResource = new Patient();
		patientResource.setId("Patient/" + patient.getId().toString());

		HumanName name = patientResource.addName();
		name.setFamily(patient.getSurname());
		name.addGiven(patient.getName());

		bundle.addEntry()
				.setFullUrl("Patient/" + patient.getId())
				.setResource(patientResource);

		Encounter encounter = new Encounter();
		encounter.setId(visit.id().toString());
		encounter.setStatus(toFhirStatus(visit.status()));
		encounter.setSubject(new Reference("Patient/" + visit.patientId()));

		Period period = new Period();
		period.setStart(Date.from(visit.visitTime().atZone(ZoneId.systemDefault()).toInstant()));
		encounter.setPeriod(period);

		bundle.addEntry()
				.setFullUrl("Encounter/" + visit.id())
				.setResource(encounter);

		return fhirContext.newJsonParser()
				.setPrettyPrint(false)
				.encodeResourceToString(bundle);
	}
}