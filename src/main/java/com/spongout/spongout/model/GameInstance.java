package com.spongout.spongout.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Getter
@Setter
public class GameInstance {

    private UUID gameId;
    private GameState currentState;
    private final Map<String, Player> players = new ConcurrentHashMap<>(); //now i see that it would be soo easier to have them in list.
    private final List<Player> alivePlayers = new CopyOnWriteArrayList<>();
    private double currentArenaRadius;
    private long roundStartTime;
    private long lastTickTime; // Used to calculate deltaTime for physics

    // An enum to represent the game's current state
    public enum GameState {
        WAITING_FOR_PLAYERS,
        COUNTDOWN,
        RUNNING,
        SUDDEN_DEATH,
        ROUND_OVER
    }

    /**
     * Constructor to initialize a new game instance with default values.
     */
    public GameInstance() {
        this.gameId = UUID.randomUUID();
        this.currentState = GameState.WAITING_FOR_PLAYERS;

        //rest should be set by gameEngine when round starts
    }

    //TODO move it to `GameEngine`
    public void spawnPlayers(double playerInitialMass) {
        // We'll spawn players in the inner 80% of the arena to give them some space.
        final double spawnRadius = this.currentArenaRadius * 0.8;
        for (Player player : players.values()) {
            // Reset all physics and state for the new round
            player.resetForNewRound();
            player.setSize(playerInitialMass);

            double spawnX, spawnY;

            // The Rejection Sampling loop
            do {
                // 1. Pick a random point in the bounding square [-spawnRadius, +spawnRadius]
                spawnX = ThreadLocalRandom.current().nextDouble(-spawnRadius, spawnRadius);
                spawnY = ThreadLocalRandom.current().nextDouble(-spawnRadius, spawnRadius);

                // 2. Check if the point is inside the circle. If not, the loop repeats.
            } while (spawnX * spawnX + spawnY * spawnY > spawnRadius * spawnRadius);

            // 3. We have a valid position. Assign it to the player.
            player.setX(spawnX);
            player.setY(spawnY);

            log.info("Spawning player {} at ({}, {})", player.getNickname(), spawnX, spawnY);
        }
    }
}
