package com.spongout.spongout.repository;

import com.spongout.spongout.model.GameInstance;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GameRepository {

    private final Map<UUID, GameInstance> activeGames = new ConcurrentHashMap<>();
    public void save(GameInstance game) {
        if (game == null || game.getGameId() == null) {
            throw new IllegalArgumentException("GameInstance or GameInstance ID cannot be null.");
        }
        activeGames.put(game.getGameId(), game);
    }

    public Optional<GameInstance> findById(UUID gameId) {
        return Optional.ofNullable(activeGames.get(gameId));
    }

    public void deleteById(UUID gameId) {
        activeGames.remove(gameId);
    }
}
