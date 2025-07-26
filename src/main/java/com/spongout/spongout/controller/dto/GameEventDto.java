package com.spongout.spongout.controller.dto;

import com.spongout.spongout.model.GameEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEventDto {

    private GameEventType eventType;

    private String message;

    private Integer value;


    public GameEventDto(GameEventType eventType, String message) {
        this(eventType, message, null);
    }

    public GameEventDto(GameEventType eventType) {
        this(eventType, null, null);
    }
}
