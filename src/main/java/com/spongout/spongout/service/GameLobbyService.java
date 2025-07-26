package com.spongout.spongout.service;

import com.spongout.spongout.config.GameConstants;
import com.spongout.spongout.controller.dto.GameStartDto;
import com.spongout.spongout.model.GameInstance;
import com.spongout.spongout.model.Player;
import com.spongout.spongout.repository.GameRepository;
import com.spongout.spongout.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class GameLobbyService {

    // --- DEPENDENCY INJECTIONS XD (I know You love it :*) ---
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    //reverse lookup for player disconnection
    private final Map<String, UUID> playerSessionToGameIdMap = new ConcurrentHashMap<>();

    //waiting room for new players
    private final List<Player> waitingRoom = new CopyOnWriteArrayList<>();

    private final Object waitingRoomLock = new Object();
    private final GameConstants gameConstants;

    /** Holds the reference to the scheduled countdown task, so it can be cancelled. */
    private volatile ScheduledFuture<?> countdownTask;

    public void handlePlayerJoin(String nickname, String sessionId) {
        synchronized (waitingRoomLock) {
            Player newPlayer = new Player(nickname, sessionId);

            waitingRoom.add(newPlayer);
            int waitingRoomSize = waitingRoom.size();

            if (waitingRoomSize >= gameConstants.getMaxPlayersInLobby()) {
                log.info("LOBBY FULL. LET'S START A GAME!");

                if (countdownTask != null) {
                    cancelCountdown();
                }

                formGameFromWaitingRoom();
            } else if (waitingRoomSize >= gameConstants.getMinPlayersToStart() && countdownTask == null) {
                log.info("{} PLAYERS WAITING. LET'S START A COUNTDOWN!", waitingRoomSize);
                startLobbyCountdown();
            } else {
                log.info("{} PLAYER/S WAITING...", waitingRoomSize);
            }
        }
    }

    public void handlePlayerDisconnect(String sessionId) {

        boolean removedFromWaitingRoom = waitingRoom.removeIf(p -> p.getSessionId().equals(sessionId));

        if (removedFromWaitingRoom) {
            if (waitingRoom.size() < gameConstants.getMinPlayersToStart()) {
                if (countdownTask != null) {
                    cancelCountdown();
                }
            }
        } else {
            var gameId = playerSessionToGameIdMap.get(sessionId);
            //if null, then player vidmo (is there this word in english?)
            if (gameId == null) {
                log.warn("Player {} is ghost! Therefore can't be removed from any game or waiting room...", sessionId);
                return;
            }
            log.info("Player {} disconnecting from game {}", sessionId, gameId);
            gameRepository.findById(gameId).ifPresent(game -> {
                game.getPlayers().remove(sessionId);
                log.info("Player {} successfully removed from Game {}", sessionId, gameId);
                // Here you could add logic to end the game if they were the last player
            });
        }
    }

    private void startLobbyCountdown() {
        // TODO: Implement this method based on the specification above.
        countdownTask = taskScheduler.schedule(
                this::formGameFromWaitingRoom,
                Instant.now().plusSeconds(gameConstants.getCountdownSeconds())
        );
    }

    private void cancelCountdown() {
        countdownTask.cancel(true);
        countdownTask = null;
        log.info("Countdown task cancelled");
    }

    private void formGameFromWaitingRoom() {
        List<Player> playersForNewGame;

        synchronized (waitingRoomLock) {
            if(waitingRoom.size() < gameConstants.getMinPlayersToStart()) {
                //it means someone left in the middle of matchmaking and handlePlayerDisconnection didnt work somehow, safety.
                countdownTask = null;
                return;
            }
            log.info("WE ARE FORMING THE GAME! YOOHOO!");
            playersForNewGame = new ArrayList<>(waitingRoom);
            waitingRoom.clear();
            countdownTask = null;
        }
        createNewGame(playersForNewGame);
    }

    private void createNewGame(List<Player> players) {
        GameInstance newGame = new GameInstance();
        UUID gameId = newGame.getGameId();
        log.info("Game {} created", gameId);

        for (Player player : players) {
            newGame.getPlayers().put(player.getSessionId(), player);
            playerSessionToGameIdMap.put(player.getSessionId(), gameId);
        }
        gameRepository.save(newGame);

        var gameStartPayload = new GameStartDto(gameId);

        for (Player player : players) {
            //it probably wont work rn
            messagingTemplate.convertAndSendToUser(player.getSessionId(), "/user/queue/private", gameStartPayload);
        }

        //todo start actual game here!
    }
}