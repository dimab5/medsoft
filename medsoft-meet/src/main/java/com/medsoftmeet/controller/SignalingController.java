package com.medsoftmeet.controller;

import com.medsoftmeet.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SignalingController {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Основной маршрут для WebRTC сигналинга (offer, answer, ice-candidate, chat, screen-share-*, ready).
	 * Сообщение маршрутизируется точечно к конкретному получателю.
	 */
	@MessageMapping("/signal")
	public void handleSignal(Signal signal) {
		if (signal == null || signal.getTo() == null || signal.getTo().isBlank()) {
			log.warn("Получен сигнал с пустым получателем, игнорируем");
			return;
		}
		log.info("Signal [{}] from={} to={}", signal.getType(), signal.getFrom(), signal.getTo());
		messagingTemplate.convertAndSend("/topic/signal/" + signal.getTo(), signal);
	}

	/**
	 * Броадкаст о входе в комнату.
	 */
	@MessageMapping("/join")
	@SendTo("/topic/join")
	public Signal join(Signal signal) {
		log.info("User joined: {}", signal.getFrom());
		return signal;
	}

	/**
	 * Броадкаст о выходе из конференции.
	 */
	@MessageMapping("/leave")
	@SendTo("/topic/leave")
	public Signal leave(Signal signal) {
		log.info("User left: {}", signal.getFrom());
		return signal;
	}
}