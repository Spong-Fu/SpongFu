package com.spongout.spongout.model;

import com.spongout.spongout.config.GameConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Game {

    private GameState currentState;
    private final Map<String, Player> players;
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
    public Game() {
        this.currentState = GameState.WAITING_FOR_PLAYERS;
        this.players = new ConcurrentHashMap<>(); // Still needs to be thread-safe
        this.currentArenaRadius = GameConstants.AREA_INITIAL_RADIUS;

        //rest should be set by gameEngine when round starts
    }
}
