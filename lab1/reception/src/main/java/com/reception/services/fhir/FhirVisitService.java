package com.reception.services.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.reception.models.VisitRequest;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class FhirVisitService {

	private final FhirContext fhirContext = FhirContext.forR4();

	public String createVisitMessage(VisitRequest request) {
		Encounter encounter = new Encounter();
		encounter.setId(UUID.randomUUID().toString());
		encounter.setStatus(Encounter.EncounterStatus.PLANNED);
		encounter.setSubject(new Reference("Patient/" + request.patientId()));

		Period period = new Period();
		period.setStart(Date.from(request.visitTime().atZone(ZoneId.systemDefault()).toInstant()));
		encounter.setPeriod(period);

		return fhirContext.newJsonParser()
				.setPrettyPrint(false)
				.encodeResourceToString(encounter);
	}
}
