package com.spongout.spongout.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.config.game")
public class GameConstants {

    public static int MIN_PLAYER_TO_START;
    public static int COUNTDOWN_SECONDS;
    public static int ROUND_MAX_SECONDS;
    public static int AREA_INITIAL_RADIUS;

    private int minPlayersToStart;
    private int countdownSeconds;
    private int roundMaxSeconds;
    private int arenaInitialRadius;

    @PostConstruct
    public void init() {
        MIN_PLAYER_TO_START = minPlayersToStart;
        COUNTDOWN_SECONDS = countdownSeconds;
        ROUND_MAX_SECONDS = roundMaxSeconds;
        AREA_INITIAL_RADIUS = arenaInitialRadius;
    }
}