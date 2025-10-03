package com.reception.services;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A03;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import com.reception.models.enums.AdministrativeSex;
import com.reception.models.enums.HL7MessageType;
import com.reception.models.enums.HL7TriggerEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class HL7Service {

    public String createPatientMessage(String firstName, String lastName, LocalDate birthDate) {
        try {
            ADT_A01 message = new ADT_A01();

            setupMSH(message.getMSH(), HL7MessageType.ADMIT_DISCHARGE_TRANSFER, HL7TriggerEvent.ADMIT);

            setupPID(message.getPID(), firstName, lastName, birthDate, null);

            PipeParser parser = new PipeParser();
            return parser.encode(message);

        } catch (HL7Exception e) {
            throw new RuntimeException("Error creating HL7 message", e);
        }
    }

    public String deletePatientMessage(UUID patientId) {
        try {
            ADT_A03 message = new ADT_A03();

            setupMSH(message.getMSH(), HL7MessageType.ADMIT_DISCHARGE_TRANSFER, HL7TriggerEvent.DISCHARGE);

            setupPID(message.getPID(), null, null, null, patientId);

            PipeParser parser = new PipeParser();
            return parser.encode(message);

        } catch (HL7Exception e) {
            throw new RuntimeException("Error creating HL7 message for delete", e);
        }
    }

    private void setupMSH(MSH msh, HL7MessageType type, HL7TriggerEvent event) throws HL7Exception {
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getSendingApplication().getNamespaceID().setValue("REGISTRATION_SERVICE");
        msh.getSendingFacility().getNamespaceID().setValue("RECEPTION");
        msh.getReceivingApplication().getNamespaceID().setValue("HIS_SYSTEM");
        msh.getReceivingFacility().getNamespaceID().setValue("HOSPITAL");

        String currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        msh.getDateTimeOfMessage().getTime().setValue(currentTime);

        msh.getMessageType().getMessageCode().setValue(type.getCode());
        msh.getMessageType().getTriggerEvent().setValue(event.getCode());
        msh.getMessageControlID().setValue(generateMessageId());
        msh.getProcessingID().getProcessingID().setValue("P");
        msh.getVersionID().getVersionID().setValue("2.5");
    }

    private void setupPID(PID pid, String firstName, String lastName, LocalDate birthDate, UUID patientId) throws HL7Exception {
        if (patientId != null) {
            pid.getPatientIdentifierList(0).getIDNumber().setValue(patientId.toString());
            pid.getPatientIdentifierList(0).getAssigningAuthority().getNamespaceID().setValue("HOSPITAL");
        }

        if (lastName != null && !lastName.isBlank()) {
            pid.getPatientName(0).getFamilyName().getSurname().setValue(lastName);
        }
        if (firstName != null && !firstName.isBlank()) {
            pid.getPatientName(0).getGivenName().setValue(firstName);
        }

        if (birthDate != null) {
            String birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            pid.getDateTimeOfBirth().getTime().setValue(birthDateStr);
        }

        pid.getAdministrativeSex().setValue(AdministrativeSex.UNKNOWN.getCode());
    }

    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}