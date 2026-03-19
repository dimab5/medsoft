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

	/**
	 * WebRTC-сигналы (offer, answer, ice-candidate, chat, screen-share-*, ready).
	 * Поле signal.to = userId получателя — маршрутизируем точечно.
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
	 * Вход в комнату.
	 * Поле signal.to = roomId — рассылаем всем участникам комнаты.
	 */
	@MessageMapping("/join")
	public void handleJoin(Signal signal) {
		if (signal == null || signal.getTo() == null || signal.getTo().isBlank()) {
			log.warn("Join без roomId, игнорируем");
			return;
		}
		log.info("User [{}] joined room [{}]", signal.getFrom(), signal.getTo());
		messagingTemplate.convertAndSend("/topic/room/" + signal.getTo(), signal);
	}

	/**
	 * Выход из комнаты.
	 * Поле signal.to = roomId — рассылаем всем участникам комнаты.
	 */
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