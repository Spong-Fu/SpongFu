package com.spongout.spongout.service;

import com.spongout.spongout.config.GameConstants;
import com.spongout.spongout.controller.dto.GameStateDto;
import com.spongout.spongout.controller.dto.PlayerStateDto;
import com.spongout.spongout.controller.payloads.GameEventPayload;
import com.spongout.spongout.model.GameEventType;
import com.spongout.spongout.model.GameInstance;
import com.spongout.spongout.model.Player;
import com.spongout.spongout.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Stores the running tasks for each game, so they can be stopped later.
     * Key: gameId, Value: The scheduled task future.
     */
    private final Map<UUID, ScheduledFuture<?>> activeGameLoops = new ConcurrentHashMap<>();
    private final GameConstants gameConstants;

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
        game.spawnPlayers(gameConstants.getPlayerStartingMass());
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
        //game.update();

        // Create a list of DTOs for each player
        List<PlayerStateDto> playerDTOs = game.getPlayers().values().stream()
                .map(player -> new PlayerStateDto(
                        player.getNickname(),
                        player.getX(),
                        player.getY(),
                        player.getSize(),
                        player.getAngle()))
                .toList();

        // 3. Create the Data Transfer Object (DTO) from the updated game state.
        GameStateDto gameStateDto = new GameStateDto(
                playerDTOs,
                game.getCurrentArenaRadius(),
                game.getCurrentState().toString()
        );

        // 4. Broadcast the complete world state to all players in this game.
        String stateDestination = String.format("/topic/game.state/%s", gameId);
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
            String winnerNickname = game.getPlayers().values().stream()
                    .map(Player::getNickname)
                    .findFirst()
                    .orElse("Nobody"); // Or handle a draw scenario

            // Broadcast a final "ROUND_WINNER" event to a different topic.
            String eventDestination = String.format("/topic/game.events/%s", gameId);
            messagingTemplate.convertAndSend(eventDestination, new GameEventPayload(GameEventType.ROUND_WINNER, winnerNickname));
        }
        log.trace("tick() end for game: {}", gameId);
    }
}