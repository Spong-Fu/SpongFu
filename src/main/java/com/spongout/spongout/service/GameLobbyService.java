package com.spongout.spongout.service;

import com.spongout.spongout.model.Player;
import com.spongout.spongout.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameLobbyService {

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    //reverse lookup for player disconnection
    private final Map<UUID, UUID> playerToGameMap = new ConcurrentHashMap<>();

    //it will be waiting room for players
    private AtomicReference<List<Player>> waitingRoom = new AtomicReference<>();

    public void findOrCreateGameToJoin(String PlayerSessionId) {
        //todo
    }

    public void createGame() {
        //todo
    }
}
