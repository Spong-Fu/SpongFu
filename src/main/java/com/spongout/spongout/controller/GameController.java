package com.spongout.spongout.controller;

import com.spongout.spongout.controller.dto.JoinRequestDto;
import com.spongout.spongout.service.GameLobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameLobbyService gameLobbyService;

    @MessageMapping("/game.find")
    public void findGame(@Payload JoinRequestDto dto, SimpMessageHeaderAccessor header) {
        log.info("[GameController.findGame] reached");
        String sessionId = header.getSessionId();
        gameLobbyService.handlePlayerJoin(dto.nickname(), sessionId);
    }

    // --- The action endpoint will be added later in Phase 3 ---
    /*
    @MessageMapping("/game/{gameId}/action")
    public void handleGameAction(...) {
        // To be implemented
    }
    */
}
