package com.argus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        return fetchResolvedLibrary(username.trim()).map(ResolvedLibrary::games);
    }

    private Optional<ResolvedLibrary> fetchResolvedLibrary(String username)
    {
        Optional<String> steamIdOpt = steamApiService.resolveUsernameUrl(username.trim());
        if (steamIdOpt.isEmpty()) {
            LOGGER.warning("Steam profile could not be resolved: " + username);
            throw new IllegalStateException("Impossibile risolvere il profilo Steam: " + username);
        }

        String steamId = steamIdOpt.get();
        LOGGER.info("Found SteamID -> " + steamId);

        Optional<List<GameDto>> gamesOpt = steamPlayerService.getOwnedGames(steamId);
        if (gamesOpt.isEmpty()) {
            return Optional.empty();
        }

        String displayName = getDisplayName(username.trim(), steamId);
        return Optional.of(new ResolvedLibrary(gamesOpt.get(), steamId, displayName));
    }

    private String getDisplayName(String input, String steamId)
    {
        Optional<String> vanityUsername = steamApiService.extractVanityUsername(input);
        if (vanityUsername.isPresent()) {
            return vanityUsername.get();
        }
        if (input.matches("^7656119\\d{10}$") || input.contains("://")) {
            return steamPlayerService.getPlayerName(steamId).orElse(steamId);
        }
        return input;
    }

    /**
     * Esegue il flusso per una lista di utenti -> unisce le librerie, traccia i proprietari e le licenze.
     */
    public List<GamePresentationDto> fetchFamilyLibrary(List<String> usernames) 
    {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, AccumulatedGameDto> familyMap = new HashMap<>();

        for (String username : usernames) {
            if (username == null || username.isBlank()) {
                continue;
            }
            Optional<ResolvedLibrary> userLibraryOpt = fetchResolvedLibrary(username.trim());
            
            if (userLibraryOpt.isPresent()) {
                ResolvedLibrary userLibrary = userLibraryOpt.get();
                for (GameDto game : userLibrary.games()) {
                    // Cerca se il gioco esiste già nella mappa della famiglia, altrimenti lo crea
                    AccumulatedGameDto acc = familyMap.computeIfAbsent(game.getAppId(), id -> new AccumulatedGameDto(
                        game.getAppId(),
                        game.getName(),
                        game.playtimeForever()
                    ));
                    
                    // Aggiunge l'utente corrente alla lista dei proprietari (assicurandosi che non ci siano duplicati)
                    acc.addOwner(userLibrary.steamId(), userLibrary.displayName());
                    
                    // Aggiorna il tempo di gioco massimo se questo utente ci ha giocato di più
                    acc.updateMaxPlaytime(game.playtimeForever());
                }
            }
        }

        // Converte gli accumulatori nei DTO finali per la UI
        List<GamePresentationDto> result = new ArrayList<>();
        for (AccumulatedGameDto acc : familyMap.values()) {
            result.add(new GamePresentationDto(
                acc.getAppId(),
                acc.getName(),
                acc.getMaxPlaytime(),
                acc.getOwners(),
                null
            ));
        }

        return result;
    }

    private record ResolvedLibrary(List<GameDto> games, String steamId, String displayName) {}
}