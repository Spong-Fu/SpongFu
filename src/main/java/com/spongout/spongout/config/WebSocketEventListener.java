package com.spongout.spongout.config;

import com.spongout.spongout.service.GameEngine;
import com.spongout.spongout.service.GameLobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameLobbyService gameLobbyService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        //dont know yet
    }

    @EventListener
    public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
        //todo: call some gameEngine disconnection method.
        gameLobbyService.handlePlayerDisconnect(event.getSessionId());
    }

}
