package com.spongout.spongout.controller.dto;

public record PlayerStateDto(
        String nickname,
        double x,
        double y,
        double size,
        double angle
) {
}
