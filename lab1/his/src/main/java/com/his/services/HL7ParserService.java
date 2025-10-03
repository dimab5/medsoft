package com.his.services;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ADT_A03;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.PipeParser;
import com.his.models.enums.HL7MessageType;
import com.his.models.requests.CreatePatientRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class HL7ParserService {

    private final PipeParser parser = new PipeParser();

    public CreatePatientRequest parseCreatePatientMessage(String hl7Message) {
        try {
            Message message = parser.parse(hl7Message);

            if (message instanceof ADT_A01) {
                ADT_A01 adtMessage = (ADT_A01) message;
                return extractPatientDataFromA01(adtMessage);
            } else {
                throw new IllegalArgumentException("Invalid HL7 message type. Expected ADT^A01");
            }

        } catch (HL7Exception e) {
            throw new RuntimeException("Error parsing HL7 message", e);
        }
    }

    public UUID parseDeletePatientMessage(String hl7Message) {
        try {
            Message message = parser.parse(hl7Message);

            if (message instanceof ADT_A03) {
                ADT_A03 adtMessage = (ADT_A03) message;
                return extractPatientIdFromA03(adtMessage);
            } else {
                throw new IllegalArgumentException("Invalid HL7 message type. Expected ADT^A03");
            }

        } catch (HL7Exception e) {
            throw new RuntimeException("Error parsing HL7 delete message", e);
        }
    }

    private CreatePatientRequest extractPatientDataFromA01(ADT_A01 message) throws HL7Exception {
        PID pid = message.getPID();

        String firstName = extractFirstName(pid);
        String lastName = extractLastName(pid);
        LocalDate birthDate = extractBirthDate(pid);

        return new CreatePatientRequest(firstName, lastName, birthDate);
    }

    private UUID extractPatientIdFromA03(ADT_A03 message) throws HL7Exception {
        PID pid = message.getPID();

        if (pid.getPatientIdentifierList().length == 0) {
            throw new IllegalArgumentException("Patient identifier is missing in the message");
        }

        String patientIdStr = pid.getPatientIdentifierList(0).getIDNumber().getValue();
        if (patientIdStr == null || patientIdStr.isBlank()) {
            throw new IllegalArgumentException("Patient identifier is empty");
        }

        try {
            return UUID.fromString(patientIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient UUID format: " + patientIdStr, e);
        }
    }

    private String extractFirstName(PID pid) throws HL7Exception {
        if (pid.getPatientName().length == 0) {
            return null;
        }

        return pid.getPatientName(0).getGivenName().getValue();
    }

    private String extractLastName(PID pid) throws HL7Exception {
        if (pid.getPatientName().length == 0) {
            return null;
        }

        return pid.getPatientName(0).getFamilyName().getSurname().getValue();
    }

    private LocalDate extractBirthDate(PID pid) throws HL7Exception {
        String birthDateStr = pid.getDateTimeOfBirth().getTime().getValue();

        if (birthDateStr == null || birthDateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid birth date format: " + birthDateStr, e);
        }
    }

    public HL7MessageType getMessageType(String hl7Message) {
        try {
            Message message = parser.parse(hl7Message);
            String messageType = message.getName();

            if (messageType.contains("ADT_A01")) {
                return HL7MessageType.ADMIT_DISCHARGE_TRANSFER;
            } else if (messageType.contains("ADT_A03")) {
                return HL7MessageType.ADMIT_DISCHARGE_TRANSFER;
            } else {
                throw new IllegalArgumentException("Unknown message type: " + messageType);
            }

        } catch (HL7Exception e) {
            throw new RuntimeException("Error determining HL7 message type", e);
        }
    }
}