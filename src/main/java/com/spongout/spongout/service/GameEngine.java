package com.spongout.spongout.service;

import com.spongout.spongout.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameEngine {

    private final GameRepository gameRepository;

    private final Map<UUID, UUID> playerToGameMap = new ConcurrentHashMap<>();


}
