package com.reception.services;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class HL7Service {

    public String createPatientMessage(String firstName, String lastName, LocalDate birthDate) {
        try {
            ADT_A01 message = new ADT_A01();

            setupMSH(message.getMSH());

            setupPID(message.getPID(), firstName, lastName, birthDate);

            PipeParser parser = new PipeParser();
            return parser.encode(message);

        } catch (HL7Exception e) {
            throw new RuntimeException("Ошибка создания HL7 сообщения", e);
        }
    }

    private void setupMSH(MSH msh) throws HL7Exception {
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getSendingApplication().getNamespaceID().setValue("REGISTRATION_SERVICE");
        msh.getSendingFacility().getNamespaceID().setValue("RECEPTION");
        msh.getReceivingApplication().getNamespaceID().setValue("HIS_SYSTEM");
        msh.getReceivingFacility().getNamespaceID().setValue("HOSPITAL");

        String currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        msh.getDateTimeOfMessage().getTime().setValue(currentTime);

        msh.getMessageType().getMessageCode().setValue("ADT");
        msh.getMessageType().getTriggerEvent().setValue("A01");
        msh.getMessageControlID().setValue(generateMessageId());
        msh.getProcessingID().getProcessingID().setValue("P");
        msh.getVersionID().getVersionID().setValue("2.5");
    }

    private void setupPID(PID pid, String firstName, String lastName, LocalDate birthDate) throws HL7Exception {
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

        pid.getAdministrativeSex().setValue("U");
    }

    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}
