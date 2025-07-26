package com.spongout.spongout.repository;

import com.spongout.spongout.model.Player;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlayerRepository {
    private final Map<String, Player> activePlayers = new ConcurrentHashMap<>();


    public void save(Player player) {
        if (player == null || player.getSessionId() == null) {
            throw new IllegalArgumentException("Player instance or Session ID cannot be null");
        }
        activePlayers.put(player.getSessionId(), player);
    }

    public Optional<Player> findById(String sessionId) {
        return Optional.ofNullable(activePlayers.get(sessionId));
    }

    public void deleteById(String sessionId) {
        activePlayers.remove(sessionId);
    }
}
