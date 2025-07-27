package com.spongout.spongout.controller;

import com.spongout.spongout.controller.dto.ActionRequestDto;
import com.spongout.spongout.controller.dto.JoinRequestDto;
import com.spongout.spongout.model.Action;
import com.spongout.spongout.service.GameExecutionService;
import com.spongout.spongout.service.GameLobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameLobbyService gameLobbyService;
    private final GameExecutionService gameExecutionService;

    @MessageMapping("/game.find")
    public void findGame(@Payload JoinRequestDto dto, SimpMessageHeaderAccessor header) {
        log.info("[GameController.findGame] reached");
        String sessionId = header.getSessionId();
        gameLobbyService.handlePlayerJoin(dto.nickname(), sessionId);
    }

    //gameId isnt needed, but leave for now - dont have time for change.
    @MessageMapping("/game/{gameId}/action")
    public void handleGameAction(SimpMessageHeaderAccessor header, @DestinationVariable UUID gameId, @Payload ActionRequestDto dto) {
       if (dto.action().equals(Action.EXPEL)) {
           gameExecutionService.requestExpelAction(header.getSessionId());
       }
    }
}
