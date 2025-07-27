package com.spongout.spongout.model;

public enum GameEventType {
    // --- Lobby Events (Sent to /user/queue/private) ---
    // NOTE: For the MVP, these are not used as the lobby is silent.
    // But they are good to have for the future.
    PLAYER_JOINED_LOBBY,
    LOBBY_COUNTDOWN_STARTED,
    LOBBY_COUNTDOWN_CANCELLED,

    // --- Game Events (Sent to /topic/{gameId}/game.events) ---
    /** A player has successfully joined a game instance. */
    PLAYER_JOINED_GAME,
    /** A player has disconnected from a game instance. */
    PLAYER_LEFT_GAME,
    /** The game round has finished and there is a winner. */
    ROUND_WINNER,
    /** The game is ready to begin (all players assigned). */
    GAME_READY,
    /** Player got eliminated */
    PLAYER_ELIMINATED,
    /** SUDDEN DEATH Started! */
    SUDDEN_DEATH
}