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
    private final GameEngine gameEngine;

    /** Holds the reference to the scheduled countdown task, so it can be cancelled. */
    private volatile ScheduledFuture<?> countdownTask;


    // ===================================================================================
    //  PUBLIC API METHODS (Called by Controllers and Event Listeners)
    // ===================================================================================

    /**
     * SPECIFICATION: This is the main entry point when a new player joins matchmaking.
     * 1.  This entire method's logic MUST be wrapped in a `synchronized (waitingRoomLock)` block
     *     to ensure that adding a player and checking the room state is an atomic operation.
     * 2.  Create a new `Player` object using the provided nickname and sessionId.
     * 3.  Add the new `Player` to the `waitingRoom` list.
     * 4.  Log the current size of the waiting room.
     * 5.  Check the number of players in `waitingRoom`:
     *     a. If the size is >= `GameConstants.MAX_PLAYERS_IN_LOBBY`:
     *        - Log that the game is starting immediately due to a full lobby.
     *        - If `countdownTask` is not null, cancel it (`countdownTask.cancel(false)`).
     *        - Call `formGameFromWaitingRoom()`.
     *     b. Else if the size is >= `GameConstants.MIN_PLAYERS_TO_START_COUNTDOWN` AND `countdownTask` is null:
     *        - Log that the silent countdown is beginning.
     *        - Call `startLobbyCountdown()`.
     */
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
                log.info("FIRST PLAYER WAITING...");
            }
        }
    }

    private void cancelCountdown() {
        countdownTask.cancel(true);
        countdownTask = null;
        log.info("Countdown task cancelled");
    }

    /**
     * SPECIFICATION: This method is called when a WebSocket connection is terminated.
     * 1.  First, attempt to remove the player from the `waitingRoom`. The `removeIf` method is a good choice.
     *     This part of the logic MUST be wrapped in a `synchronized (waitingRoomLock)` block.
     *     a. If the player was successfully removed from `waitingRoom`:
     *        - Log the removal.
     *        - Check if the `waitingRoom` size is now LESS THAN `GameConstants.MIN_PLAYERS_TO_START_COUNTDOWN`.
     *        - If it is, and if `countdownTask` is not null, you MUST cancel the countdown (`countdownTask.cancel(false)`)
     *          and set `countdownTask = null;`. Log this cancellation.
     *        - `return` from the method, as the cleanup is done.
     * 2.  If the player was not in the waiting room, they must be in an active game.
     *     a. Use `playerSessionToGameIdMap.remove(sessionId)` to get the `gameId` and remove the mapping.
     *     b. If the returned `gameId` is not null:
     *        - Log the player's disconnection from the specific game.
     *        - (Future work) Find the game in `gameRepository` and remove the player from that game's internal list.
     */
    public void handlePlayerDisconnect(String sessionId) {
        Optional<Player> optPlayer = playerRepository.findById(sessionId);
        if (optPlayer.isEmpty()) return;

        Player player = optPlayer.get();
        //if removed from waitingRoom, then there shouldnt be this player in any active game, so we dont have to check it
        if (waitingRoom.remove(player)) {
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
            //yes, i put this spaghetti on purpose :>
            gameRepository.findById(gameId).orElseThrow().getPlayers().remove(sessionId);
        }
    }


    // ===================================================================================
    //  INTERNAL LOGIC (Private helper methods)
    // ===================================================================================

    /**
     * SPECIFICATION: Schedules the formation of a game after a delay.
     * 1.  Use `taskScheduler.schedule(Runnable, Instant)` to schedule a task.
     * 2.  The `Runnable` argument should be a method reference to `this::formGameFromWaitingRoom`.
     * 3.  The `Instant` argument should be `Instant.now().plusSeconds(GameConstants.LOBBY_COUNTDOWN_SECONDS)`.
     * 4.  Store the `ScheduledFuture<?>` object returned by the scheduler into the `this.countdownTask` field.
     */
    private void startLobbyCountdown() {
        // TODO: Implement this method based on the specification above.
        countdownTask = taskScheduler.schedule(
                this::formGameFromWaitingRoom,
                Instant.now().plusSeconds(gameConstants.getCountdownSeconds())
        );
    }

    /**
     * SPECIFICATION: Creates a game from the players in the waiting room.
     * 1.  Declare a new `List<Player> playersForNewGame = new ArrayList<>();`.
     * 2.  The section that modifies the `waitingRoom` MUST be wrapped in a `synchronized (waitingRoomLock)` block.
     *     a. Inside the lock, perform a final check: if `waitingRoom.size()` is now less than `MIN_PLAYERS_TO_START_COUNTDOWN`,
     *        it means players left during the countdown. Log this, set `countdownTask = null`, and `return`.
     *     b. If enough players exist, log that the game is being formed.
     *     c. Add all players from `waitingRoom` into your `playersForNewGame` list.
     *     d. Clear the waiting room: `waitingRoom.clear()`.
     *     e. Reset the countdown task: `countdownTask = null;`.
     * 3.  Outside the lock, check if `playersForNewGame` is not empty. If it has players, call `createNewGame(playersForNewGame)`.
     */
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

    /**
     * SPECIFICATION: The final step. Creates the GameInstance object, saves it, and notifies players.
     * 1.  Create a new `GameInstance` instance: `GameInstance newGame = new GameInstance();`.
     * 2.  Get its ID: `UUID gameId = newGame.getGameId();`. Log the creation.
     * 3.  Iterate through the `players` list provided as an argument. For each player:
     *     a. Add the player to the `newGame`'s internal player map (`newGame.getPlayers().put(...)`).
     *     b. Add an entry to the `playerSessionToGameIdMap` mapping the player's `sessionId` to the `gameId`.
     * 4.  Save the `newGame` object using `gameRepository.save(newGame)`.
     * 5.  Iterate through the `players` list again. For each player:
     *     a. Use `messagingTemplate.convertAndSendToUser()` to send them a `GameAssignmentPayload`.
     *     b. The destination must be their private queue (`/user/queue/private`).
     * 6.  Log that all players have been notified and assigned to the game.
     * 7.  (Leave a comment here as a placeholder for the future `gameExecutionService.startRound(newGame);` call).
     */
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
            messagingTemplate.convertAndSendToUser(player.getNickname(), "/user/queue/private", gameStartPayload);
        }

        //todo start actual game here!
    }
}