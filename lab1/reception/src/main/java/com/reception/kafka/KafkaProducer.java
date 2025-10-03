package com.reception.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);

        log.info("Message %s send successfully".formatted(message));
    }

    public void sendMessage(String topic, Object message) {
        sendMessage(topic, null, message);
    }
}