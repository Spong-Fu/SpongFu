package com.spongout.spongout.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.config.game")
public class GameConstants {
    private int minPlayersToStart;
    private int maxPlayersInLobby;
    private int countdownSeconds;
    private int roundMaxSeconds;
    private double arenaInitialRadius;
    private double playerStartingMass;
    private long tickRateMs;
    
    private double playerGrowthRate; // Mass per second
    private double playerSpinRateRad; // Radians per second
    private double frictionFactor; // Multiplier per frame
    private long suddenDeathMs; // Time in ms before sudden death starts
    private double arenaShrinkRate; // Radius units per second
}