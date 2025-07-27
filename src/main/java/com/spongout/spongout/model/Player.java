package com.spongout.spongout.model;


import lombok.Getter;
import lombok.Setter;

import java.security.Principal;

@Getter
@Setter
public class Player implements Principal {

    private final String nickname;
    private final String sessionId;

    private int score; //for future usage
    private boolean isEliminated;

    //Physics
    private double x, y;
    private double velocityX, velocityY;
    private double size;
    private double angle;

    private volatile boolean goingToExpel; //not sure if this will be needed.

    public Player(String nickname, String sessionId) {
        this.score = 0;
        this.nickname = nickname;
        this.sessionId = sessionId;

        //other fields will be set @spawn
    }
    /**
     * Resets the player's state for the start of a new round.
     * The score and identity are not reset.
     */
    public void resetForNewRound() {
        this.isEliminated = false;
        this.goingToExpel = false;
        this.velocityX = 0;
        this.velocityY = 0;
        // Position (x, y) and initial size should be set by the GameEngine when spawning.
    }

    @Override
    public String getName() {
        return this.getNickname();
    }

    public void changeSize(double value) {
        this.size += value;
    }

    public void changeAngle(double value) {
        this.angle += value;
    }
}

