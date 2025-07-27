package com.spongout.spongout.service;

import com.spongout.spongout.config.GameConstants;
import com.spongout.spongout.config.WebSocketConstants;
import com.spongout.spongout.controller.dto.GameStateDto;
import com.spongout.spongout.controller.dto.PlayerStateDto;
import com.spongout.spongout.controller.dto.GameEventDto;
import com.spongout.spongout.model.GameEventType;
import com.spongout.spongout.model.GameInstance;
import com.spongout.spongout.model.Player;
import com.spongout.spongout.repository.GameRepository;
import com.spongout.spongout.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameExecutionService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameEngine gameEngine;

    /**
     * Stores the running tasks for each game, so they can be stopped later.
     * Key: gameId, Value: The scheduled task future.
     */
    private final Map<UUID, ScheduledFuture<?>> activeGameLoops = new ConcurrentHashMap<>();
    private final GameConstants gameConstants;

    /**
     * A public method that the GameController will call to register a player's action.
     * @param sessionId The ID of the player who wants to perform the action.
     */
    public void requestExpelAction(String sessionId) {
        Optional<Player> playerOpt = playerRepository.findById(sessionId);

        if (playerOpt.isEmpty()) {
            log.warn("Player {} isn't visible in playerRepository");
            return;
        }

        Player player = playerOpt.get();

        if (!player.isEliminated()) {
            // This flag will be picked up by the `update()` method on the next tick.
            player.setGoingToExpel(true);
        }
    }

    public void startRound(UUID gameId) {
        // TODO: Implement this method.
        log.info("GAME {} IS STARTING!", gameId);
        Optional<GameInstance> currentGameOpt = gameRepository.findById(gameId);

        if (currentGameOpt.isEmpty()) {
            log.error("Game with id {} can't be found in DB. It shouldn't happen", gameId);
        }

        GameInstance game = currentGameOpt.get();

        game.setCurrentArenaRadius(gameConstants.getArenaInitialRadius());
        game.setCurrentState(GameInstance.GameState.RUNNING);
        game.setRoundStartTime(System.currentTimeMillis());
        game.setLastTickTime(System.currentTimeMillis());
        game.spawnPlayers(gameConstants.getPlayerStartingSize());
        log.debug("InitialArenaRadius: {}", gameConstants.getArenaInitialRadius());
        log.debug("GAME SET-UP!, ID: {}", gameId);
        var gameLoop = taskScheduler.scheduleAtFixedRate(
                () -> tick(gameId),
                gameConstants.getTickRateMs()
        );
        activeGameLoops.put(gameId, gameLoop);
        log.debug("GAME STARTED! ID: {}", gameId);
    }

    /**
     * Performs a single tick of the game loop for a given game.
     * This method is called repeatedly by the TaskScheduler.
     *
     * @param gameId The ID of the game to update.
     */
    private void tick(UUID gameId) {
        MDC.put("GAME", String.valueOf(gameId.hashCode()));
        log.trace("tick() start for game: {}", gameId);
        // 1. Find the game instance. This is the most critical step.
        var optionalGame = gameRepository.findById(gameId);

        // --- HANDLE CASE WHERE GAME NO LONGER EXISTS ---
        if (optionalGame.isEmpty()) {
            log.warn("Tick called for non-existent or already terminated game: {}. Stopping its loop.", gameId);
            // Clean up the zombie task from the map to prevent memory leaks.
            ScheduledFuture<?> gameLoopTask = activeGameLoops.remove(gameId);
            if (gameLoopTask != null) {
                gameLoopTask.cancel(false);
            }
            return;
        }

        GameInstance game = optionalGame.get();
        log.trace("Game instance found: {}, state: {}", gameId, game.getCurrentState());

        // 2. Delegate all physics and game logic calculations to the game instance.
        gameEngine.update(game);

        // Create a list of DTOs for each player
        List<PlayerStateDto> playerDTOs = game.getPlayers().values().stream()
                .map(player -> new PlayerStateDto(
                        player.isEliminated(),
                        player.getNickname(),
                        player.getX(),
                        player.getY(),
                        player.getSize(),
                        player.getAngle()))
                .toList();

        // 3. Create the Data Transfer Object (DTO) from the updated game state.
        GameStateDto gameStateDto = new GameStateDto(
                playerDTOs,
                game.getCurrentArenaRadius()
        );

        // 4. Broadcast the complete world state to all players in this game.
        String stateDestination = WebSocketConstants.GAME_STATE_TOPIC + gameId;
        log.trace("Broadcasting game state to: {}", stateDestination);
        messagingTemplate.convertAndSend(stateDestination, gameStateDto);

        // 5. Check for the win condition and handle the end of the round.
        if (game.getCurrentState() == GameInstance.GameState.ROUND_OVER) {
            log.info("Game {} has ended. Stopping loop and broadcasting winner.", gameId);

            // Stop the game loop for this game.
            ScheduledFuture<?> gameLoopTask = activeGameLoops.remove(gameId);
            if (gameLoopTask != null) {
                gameLoopTask.cancel(false);
            }

            // Find the winner (should be the only player left).
            if (game.getAlivePlayers().size() > 1) {
                log.error("Error! Game finished with more that 1 player alive!");
            }
            Player winner = game.getAlivePlayers().getFirst();
            winner.addWin(game.getPlayers().size());

            // Broadcast a final "ROUND_WINNER" event to a different topic.
            String eventDestination = WebSocketConstants.GAME_EVENTS_TOPIC + gameId;
            messagingTemplate.convertAndSend(eventDestination, new GameEventDto(GameEventType.ROUND_WINNER, winner.getNickname()));
            log.info("{} WON!", winner.getNickname());

            //Move players to lobby
//            for (Player player : game.getPlayers().values()) {
//                // cant use for now - circular dependency
//                // but will make it work :)
//               lobbyService.handlePlayerReJoin(player);
//            }
        }
        log.trace("tick() end for game: {}", gameId);
    }
}