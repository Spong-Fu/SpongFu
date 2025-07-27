package com.spongout.spongout.controller.dto;

import java.util.List;

public record GameStateDto(
        List<PlayerStateDto> players,
        double arenaRadius
) {
}
