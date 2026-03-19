package com.medsoftmeet.controller;

import com.medsoftmeet.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SignalingController {

	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/signal")
	public void handleSignal(Signal signal) {
		if (signal == null || signal.getTo() == null || signal.getTo().isBlank()) {
			log.warn("Получен сигнал с пустым получателем, игнорируем");
			return;
		}
		log.info("Signal [{}] from={} to={}", signal.getType(), signal.getFrom(), signal.getTo());
		messagingTemplate.convertAndSend("/topic/signal/" + signal.getTo(), signal);
	}

	@MessageMapping("/join")
	public void handleJoin(Signal signal) {
		if (signal == null || signal.getTo() == null || signal.getTo().isBlank()) {
			log.warn("Join без roomId, игнорируем");
			return;
		}
		log.info("User [{}] joined room [{}]", signal.getFrom(), signal.getTo());
		messagingTemplate.convertAndSend("/topic/room/" + signal.getTo(), signal);
	}

	@MessageMapping("/leave")
	public void handleLeave(Signal signal) {
		if (signal == null || signal.getTo() == null || signal.getTo().isBlank()) {
			log.warn("Leave без roomId, игнорируем");
			return;
		}
		log.info("User [{}] left room [{}]", signal.getFrom(), signal.getTo());
		messagingTemplate.convertAndSend("/topic/room/" + signal.getTo(), signal);
	}
}