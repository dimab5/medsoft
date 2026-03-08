package com.medsoftmeet.controller;

import com.medsoftmeet.model.Signal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/signal")
    public void handleSignal(Signal signal) {
        System.out.println("Signal: " + signal.getType() + " from " +
                signal.getFrom() + " to " + signal.getTo());

        messagingTemplate.convertAndSend("/topic/signal/" + signal.getTo(), signal);
    }

    @MessageMapping("/join")
    @SendTo("/topic/join")
    public Signal join(Signal signal) {
        System.out.println("User joined: " + signal.getFrom());
        return signal;
    }

    @MessageMapping("/leave")
    @SendTo("/topic/leave")
    public Signal leave(Signal signal) {
        System.out.println("User left: " + signal.getFrom());
        return signal;
    }
}