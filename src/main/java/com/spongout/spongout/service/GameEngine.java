package com.spongout.spongout.service;

import com.spongout.spongout.config.GameConstants;
import com.spongout.spongout.config.WebSocketConstants;
import com.spongout.spongout.controller.dto.GameEventDto;
import com.spongout.spongout.model.GameEventType;
import com.spongout.spongout.model.GameInstance;
import com.spongout.spongout.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
        MDC.put("GAME", game.getGameId().toString());

        // Calculate Delta Time
        long now = System.currentTimeMillis();
        double deltaTime = (now - game.getLastTickTime()) / 1000.0;
        game.setLastTickTime(now);

        long currentGameTime = now - game.getRoundStartTime();

        List<Player> players = game.getAlivePlayers();

        updatePlayers(players, deltaTime);

        //collision logic
        collidePlayers(game, players);

        //Game State Updates
        updateGameState(game, deltaTime, currentGameTime);
    }

    private void updatePlayers(List<Player> players, double deltaTime) {
        //single player updates logic
        //changing size (growing)
        //changing angle for expel
        //expelling players if action key pressed
        for (Player player : players) {
            player.changeSize(gameConstants.getPlayerGrowthRate() * deltaTime);
            player.changeAngle(gameConstants.getPlayerSpinRateRad() * deltaTime);

            // Move players based on their velocity
            movePlayer(player, deltaTime);

            if (player.isGoingToExpel()) {
                launchPlayer(player);
            }
        }
    }

    private void movePlayer(Player player, double deltaTime) {
        // Update position based on velocity
        player.setX(player.getX() + player.getVelocityX() * deltaTime);
        player.setY(player.getY() + player.getVelocityY() * deltaTime);

        // Apply friction to gradually slow down the player
        // Friction is applied as a multiplier per frame, converted to per-second basis
        double frictionMultiplier = Math.pow(gameConstants.getFrictionFactor(), deltaTime);
        player.setVelocityX(player.getVelocityX() * frictionMultiplier);
        player.setVelocityY(player.getVelocityY() * frictionMultiplier);

        // Optional: Stop very small velocities to prevent floating point precision issues
        if (Math.abs(player.getVelocityX()) < 0.01) {
            player.setVelocityX(0);
        }
        if (Math.abs(player.getVelocityY()) < 0.01) {
            player.setVelocityY(0);
        }
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
//        player.setVelocityX(player.getVelocityX() + launchVelocityX);
//        player.setVelocityY(player.getVelocityY() + launchVelocityY);
        // lets try different approach
        player.setVelocityX(launchVelocityX);
        player.setVelocityY(launchVelocityY);

        // 5. Reset the player's size to the initial value
//        player.setSize(gameConstants.getPlayerStartingSize());

        // 6. Reset the flag so they don't launch again on the next frame
        player.setGoingToExpel(false);

        log.info("Player {} expelled with power {}!", player.getNickname(), launchSpeed);
    }

    private void collidePlayers(GameInstance game, List<Player> players) {

        for (Player player : players) {

            // Wall collision check (circular arena) - using squared distance
            double distanceSquaredFromCenter = player.getX() * player.getX() + player.getY() * player.getY();
            double arenaRadiusMinusPlayerSize = game.getCurrentArenaRadius() - player.getSize();
            if (distanceSquaredFromCenter > arenaRadiusMinusPlayerSize * arenaRadiusMinusPlayerSize) {
                eliminatePlayer(game, player);
            }

            // Player-vs-Player collision
            for (Player player2 : players) {
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
                    // Collision detected
                    double distance = Math.sqrt(distanceSquared);
                    handlePlayerCollision(player, player2, dx, dy, distance);
                }
            }
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

    private void updateGameState(GameInstance game, double deltaTime, long currentGameTime) {
        //check if SUDDEN_DEATH ON
        if (game.getCurrentState().equals(GameInstance.GameState.SUDDEN_DEATH)) {
            var arenaRadius = game.getCurrentArenaRadius() - (gameConstants.getArenaShrinkRate() * deltaTime);
            game.setCurrentArenaRadius(arenaRadius);

            //CHECK IF SUDDEN_DEATH SHOULD BE ON :}
        } else if (game.getCurrentState().equals(GameInstance.GameState.RUNNING) && currentGameTime > gameConstants.getSuddenDeathMs()) {
            game.setCurrentState(GameInstance.GameState.SUDDEN_DEATH);
            var payload = new GameEventDto(GameEventType.SUDDEN_DEATH);
            simpMessagingTemplate.convertAndSend(WebSocketConstants.GAME_EVENTS_TOPIC + game.getGameId(), payload);
        }
        //CHECK IF ROUND_OVER
        if (game.getAlivePlayers().size() <= 1) {
            game.setCurrentState(GameInstance.GameState.ROUND_OVER);
        }
    }

    private void eliminatePlayer(GameInstance game, Player player) {
        var payload = new GameEventDto(GameEventType.PLAYER_ELIMINATED, player.getNickname());
        simpMessagingTemplate.convertAndSend(WebSocketConstants.GAME_EVENTS_TOPIC + game.getGameId(), payload);
        player.setEliminated(true);
        game.getAlivePlayers().remove(player);
    }
}
