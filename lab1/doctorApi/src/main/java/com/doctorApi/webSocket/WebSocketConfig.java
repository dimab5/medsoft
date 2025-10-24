package com.doctorApi.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@RequiredArgsConstructor
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private final DoctorWebSocketHandler doctorWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(doctorWebSocketHandler, "/ws/visits")
                .setAllowedOriginPatterns(
                        "https://localhost:*",
                        "wss://localhost:*",
                        "http://localhost:*",
                        "ws://localhost:*"
                );
    }
}