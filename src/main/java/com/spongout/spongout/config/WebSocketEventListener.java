package com.spongout.spongout.config;

import com.spongout.spongout.service.GameEngine;
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

    private final GameEngine gameEngine;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        //dont know yet
    }

    @EventListener
    public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
        //todo: call some gameEngine disconnection method.
    }

}
