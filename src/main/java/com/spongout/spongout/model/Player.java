package com.spongout.spongout.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {

    private final String nickname;
    private final String sessionId;

    private int score; //for future usage
    private boolean isEliminated;

    //Physics
    private double x, y;
    private double velocityX, velocityY;
    private double mass;
    private double angle;

    private volatile boolean wantsToExpel; //not sure if this will be needed.

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
        this.wantsToExpel = false;
        this.velocityX = 0;
        this.velocityY = 0;
        // Position (x, y) and initial mass should be set by the GameEngine when spawning.
    }

}

