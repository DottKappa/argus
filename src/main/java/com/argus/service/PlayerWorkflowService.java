package com.argus.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PlayerWorkflowService {
    
    private static final Logger LOGGER = Logger.getLogger(PlayerWorkflowService.class.getName());

    private final SteamApiService steamApiService;
    private final SteamPlayerService steamPlayerService;

    public PlayerWorkflowService(SteamApiService steamApiService, SteamPlayerService steamPlayerService)
    {
        this.steamApiService = steamApiService;
        this.steamPlayerService = steamPlayerService;
    }

    /**
     * Esegue il flusso -> cerca steamID -> recupera libreria
     */
    public Optional<List<GameDto>> fetchLibraryForUsername(String username)
    {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        Optional<String> steamIdOpt = steamApiService.resolveUsernameUrl(username.trim());
        if (steamIdOpt.isEmpty()) {
            LOGGER.warning("Username not found " + username);
            return Optional.empty();
        }

        String steamId = steamIdOpt.get();
        LOGGER.info("Found SteamID -> " + steamId);

        return steamPlayerService.getOwnedGames(steamId);
    }
}
