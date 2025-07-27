package com.spongout.spongout.service;

import com.spongout.spongout.config.GameConstants;
import com.spongout.spongout.model.GameInstance;
import com.spongout.spongout.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameEngine {

    private final GameConstants gameConstants;

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * Advances the game state by one frame. This is the core game loop logic.
     */
    public void update(GameInstance game) {
        Map<String, Player> players = game.getPlayers();

        // --- 1. Calculate Delta Time ---
        long now = System.currentTimeMillis();
        double deltaTime = (now - game.getLastTickTime()) / 1000.0;
        game.setLastTickTime(now);

        long currentGameTime = now - game.getRoundStartTime();


        //parallel loop for
        players.values().parallelStream().forEach(player -> {
            player.changeSize(gameConstants.getPlayerGrowthRate() * deltaTime);
            player.changeAngle(gameConstants.getPlayerSpinRateRad() * deltaTime);
            if (player.isGoingToExpel()) {
                launchPlayer(player);
            }
        });

        // --- 3. Handle Collisions ---
        // TODO: Implement collision logic.
        //  a. Player-vs-Wall: Loop through players. If their distance from (0,0) is greater
        //     than currentArenaRadius, mark them as isEliminated = true.
        //  b. Player-vs-Player (The Hard Part): Use a nested loop to check every player against
        //     every other player. If they collide, calculate and apply new velocities using
        //     2D elastic collision formulas.

        players.values().parallelStream().forEach(player -> {

            // Wall collision check (circular arena) - using squared distance
            double distanceSquaredFromCenter = player.getX() * player.getX() + player.getY() * player.getY();
            double arenaRadiusMinusPlayerSize = game.getCurrentArenaRadius() - player.getSize();
            if (distanceSquaredFromCenter > arenaRadiusMinusPlayerSize * arenaRadiusMinusPlayerSize) {
                player.setEliminated(true);
            }

            // Player-vs-Player collision
            for (Player player2 : players.values()) {
                if (player.getSessionId().equals(player2.getSessionId())) {
                    continue; // Skip same player
                }

                // Calculate squared distance between player centers
                double dx = player2.getX() - player.getX();
                double dy = player2.getY() - player.getY();
                double distanceSquared = dx * dx + dy * dy;

                // Check if collision occurs (squared sum of radii)
                double minDistance = player.getSize() + player2.getSize();
                double minDistanceSquared = minDistance * minDistance;

                if (distanceSquared < minDistanceSquared && distanceSquared > 0) {
                    // Collision detected - we still need actual distance for physics calculations
                    double distance = Math.sqrt(distanceSquared);
                    handlePlayerCollision(player, player2, dx, dy, distance);
                }
            }
        });

        // --- 4. Update Game State & Cleanup ---
        // TODO:
        //  a. Remove eliminated players from the `players` map.
        //  b. If in SUDDEN_DEATH state, shrink the arena:
        //     currentArenaRadius -= ARENA_SHRINK_RATE * deltaTime;
        //  c. Check for Sudden Death trigger: If the game state is RUNNING and
        //     (System.currentTimeMillis() - roundStartTime > SUDDEN_DEATH_MS),
        //     change the state to SUDDEN_DEATH.
        //  d. Check for Win Condition: If `players.size() <= 1`, set currentState to ROUND_OVER.

        for(Player player : players.values()) {
            if (player.isEliminated()) {
                game.getPlayers().remove(player.getSessionId());
                //TODO: send payload to events topic
            }
        }

        if (game.getCurrentState().equals(GameInstance.GameState.SUDDEN_DEATH)) {
            var arenaRadius = game.getCurrentArenaRadius() - (gameConstants.getArenaShrinkRate() * deltaTime);
            game.setCurrentArenaRadius(arenaRadius);
        } else if (
                        game.getCurrentState().equals(GameInstance.GameState.RUNNING) &&
                        currentGameTime > gameConstants.getSuddenDeathMs()) {
            game.setCurrentState(GameInstance.GameState.SUDDEN_DEATH);
        }

        if (players.size() <= 1) {
            game.setCurrentState(GameInstance.GameState.ROUND_OVER);
            //TODO: send payload to events topic
        }
    }

    private void handlePlayerCollision(Player p1, Player p2, double dx, double dy, double distance) {
        // Normalize collision vector (distance already calculated in caller)
        double nx = dx / distance;
        double ny = dy / distance;

        // Relative velocity
        double dvx = p2.getVelocityX() - p1.getVelocityX();
        double dvy = p2.getVelocityY() - p1.getVelocityY();

        // Relative velocity along collision normal
        double dvn = dvx * nx + dvy * ny;

        // Do not resolve if velocities are separating
        if (dvn > 0) return;

        // Collision impulse (assuming equal mass for simplicity)
        double impulse = 2 * dvn / 2; // (mass1 + mass2) = 2 for equal masses

        // Update velocities
        p1.setVelocityX(p1.getVelocityX() + impulse * nx);
        p1.setVelocityY(p1.getVelocityY() + impulse * ny);
        p2.setVelocityX(p2.getVelocityX() - impulse * nx);
        p2.setVelocityY(p2.getVelocityY() - impulse * ny);

        // Separate overlapping players
        double overlap = (p1.getSize() + p2.getSize()) - distance;
        double separationDistance = overlap / 2;

        p1.setX(p1.getX() - separationDistance * nx);
        p1.setY(p1.getY() - separationDistance * ny);
        p2.setX(p2.getX() + separationDistance * nx);
        p2.setY(p2.getY() + separationDistance * ny);
    }

    private void launchPlayer(Player player) {
        // 1. Calculate the magnitude (speed) of the launch
        double launchSpeed = player.getSize() * gameConstants.getLaunchPowerMultiplier();

        // 2. Get the player's angle in radians
        double angle = player.getAngle();

        // 3. Calculate the X and Y components of the launch velocity
        double launchVelocityX = Math.cos(angle) * launchSpeed;
        double launchVelocityY = Math.sin(angle) * launchSpeed;

        // 4. Add the launch velocity to the player's current velocity (as an impulse)
        player.setVelocityX(player.getVelocityX() + launchVelocityX);
        player.setVelocityY(player.getVelocityY() + launchVelocityY);

        // 5. Reset the player's size to the initial value
        player.setSize(gameConstants.getPlayerStartingMass());

        // 6. Reset the flag so they don't launch again on the next frame
        player.setGoingToExpel(false);

        log.info("Player {} expelled with power {}!", player.getNickname(), launchSpeed);
    }
}
