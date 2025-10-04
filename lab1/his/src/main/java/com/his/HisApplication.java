package com.his;

import com.his.services.HL7ParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
public class HisApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HisApplication.class);
/*
        HL7ParserService parserService = context.getBean(HL7ParserService.class);

        String hl7Message = "MSH|^~\\&|REGISTRATION_SERVICE|RECEPTION|HIS_SYSTEM|HOSPITAL|20251004154949||ADT^A01|MSG_1759582189216_252|P|2.5\rPID|||||string^string||20251004|U\r";

        var result = parserService.parseCreatePatientMessage(hl7Message);
        log.info("Parsed message: " + result.toString());*/
    }
}