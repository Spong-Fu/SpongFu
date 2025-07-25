package com.spongout.spongout.repository;

import com.spongout.spongout.model.Game;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GameRepository {

    private final Map<UUID, Game> activeGames = new ConcurrentHashMap<>();
    private final Map<UUID, Game> gamesToConnect = new ConcurrentHashMap<>();

    void save(Game game) {
        if (game == null || game.getGameId() == null) {
            throw new IllegalArgumentException("Game or Game ID cannot be null.");
        }
        activeGames.put(game.getGameId(), game);
        if (game.getCurrentState().equals(Game.GameState.WAITING_FOR_PLAYERS)) {
            gamesToConnect.put(game.getGameId(), game);
        } else {
            gamesToConnect.remove(game.getGameId());
        }
    }

    Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(activeGames.get(gameId));
    }

    void deleteById(UUID gameId) {
        activeGames.remove(gameId);
    }
}
