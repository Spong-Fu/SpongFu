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

    private static final String USER_PRIVATE_QUEUE = "/queue/private-user";

    // --- DEPENDENCY INJECTIONS XD (I know You love it) ---
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
            log.info("Player joining: nickname={}, sessionId={}", nickname, sessionId);
            Player newPlayer = new Player(nickname, sessionId);

            waitingRoom.add(newPlayer);
            int waitingRoomSize = waitingRoom.size();
            log.debug("Current waiting room size: {}", waitingRoomSize);

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
        log.info("Handling player disconnect for sessionId: {}", sessionId);

        boolean removedFromWaitingRoom = waitingRoom.removeIf(p -> p.getSessionId().equals(sessionId));

        if (removedFromWaitingRoom) {
            log.info("Player {} removed from waiting room. Current waiting room size: {}", sessionId, waitingRoom.size());
            if (waitingRoom.size() < gameConstants.getMinPlayersToStart()) {
                log.info("Players count {} is below minimum {} required to start", waitingRoom.size(), gameConstants.getMinPlayersToStart());
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
        log.info("Starting lobby countdown for {} seconds", gameConstants.getCountdownSeconds());
        countdownTask = taskScheduler.schedule(
                this::formGameFromWaitingRoom,
                Instant.now().plusSeconds(gameConstants.getCountdownSeconds())
        );
        log.debug("Countdown task scheduled: {}", countdownTask);
    }

    private void cancelCountdown() {
        countdownTask.cancel(true);
        countdownTask = null;
        log.info("Countdown task cancelled");
    }

    private void formGameFromWaitingRoom() {
        log.info("Attempting to form a game from waiting room with {} players", waitingRoom.size());
        List<Player> playersForNewGame;

        synchronized (waitingRoomLock) {
            if(waitingRoom.size() < gameConstants.getMinPlayersToStart()) {
                //it means someone left in the middle of matchmaking and handlePlayerDisconnection didnt work somehow, safety.
                log.warn("Cannot form game - not enough players: {} (min required: {})", waitingRoom.size(), gameConstants.getMinPlayersToStart());
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
        log.info("Creating new game with {} players", players.size());
        GameInstance newGame = new GameInstance();
        UUID gameId = newGame.getGameId();
        log.info("Game {} created", gameId);

        for (Player player : players) {
            log.info("Adding player {} (sessionId: {}) to game {}", player.getNickname(), player.getSessionId(), gameId);
            newGame.getPlayers().put(player.getSessionId(), player);
            playerSessionToGameIdMap.put(player.getSessionId(), gameId);
        }
        log.info("Saving game {} to repository", gameId);
        gameRepository.save(newGame);

        var gameStartPayload = new GameStartDto(gameId);
        log.info("Created GameStartDto payload with gameId: {}", gameId);

        for (Player player : players) {
            log.info("Sending game start notification to player {} (sessionId: {})", player.getNickname(), player.getSessionId());
            //it probably wont work rn
            messagingTemplate.convertAndSend(USER_PRIVATE_QUEUE + player.getSessionId(), gameStartPayload);
        }

        //todo start actual game here!
    }
}