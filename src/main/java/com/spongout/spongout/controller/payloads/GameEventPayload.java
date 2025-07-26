package com.spongout.spongout.controller.payloads;

import com.spongout.spongout.model.GameEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEventPayload {

    private GameEventType eventType;

    private String message;

    private Integer value;


    public GameEventPayload(GameEventType eventType, String message) {
        this(eventType, message, null);
    }

    public GameEventPayload(GameEventType eventType) {
        this(eventType, null, null);
    }
}
