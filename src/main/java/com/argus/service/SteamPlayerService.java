package com.argus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class SteamPlayerService {
    
    private static final Logger LOGGER = Logger.getLogger(SteamPlayerService.class.getName());
    private static final String OWNED_GAMES_URL = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&include_appinfo=true&include_played_free_games=true&format=json";

    private final HttpClient httpClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public SteamPlayerService(HttpClient httpClient, String apiKey)
    {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    public Optional<List<GameDto>> getOwnedGames(String steamId)
    {
        if (steamId == null || steamId.isBlank()) {
            return Optional.empty();
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("STEAM_API_KEY non configurata.");
        }

        try {
            String url = String.format(OWNED_GAMES_URL, apiKey, steamId);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Steam GetOwnedGames ha risposto HTTP " + response.statusCode()
                        + " per " + steamId + ".");
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode gamesNode = rootNode.path("response").path("games");
            if (gamesNode.isArray()) {
                List<GameDto> games = objectMapper.readerForListOf(GameDto.class).readValue(gamesNode);
                return Optional.of(games);
            }
            throw new IllegalStateException("Steam non ha restituito la lista giochi per " + steamId
                    + ". Verifica la privacy dei dettagli dei giochi e la risposta dell'API.");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.severe("Exception on searching games for " + steamId + " -> " + e.getMessage());
            throw new IllegalStateException("Errore durante la richiesta GetOwnedGames a Steam per "
                    + steamId + ": " + e.getMessage(), e);
        }
    }

    public Optional<String> getPlayerName(String steamId)
    {
        if (steamId == null || steamId.isBlank() || apiKey == null || apiKey.isBlank()) {
            LOGGER.warning("Cannot look up Steam player name: SteamID64 or API key is missing.");
            return Optional.empty();
        }

        String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&steamids=" + URLEncoder.encode(steamId, StandardCharsets.UTF_8)
                + "&format=json";
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warning("Steam GetPlayerSummaries returned HTTP " + response.statusCode()
                        + " for " + steamId);
                return Optional.empty();
            }

            JsonNode players = objectMapper.readTree(response.body()).path("response").path("players");
            if (players.isArray() && !players.isEmpty()) {
                String playerName = players.get(0).path("personaname").asText();
                if (!playerName.isBlank()) {
                    return Optional.of(playerName);
                }
            }
            LOGGER.warning("Steam did not return a player name for " + steamId);
        } catch (Exception e) {
            LOGGER.warning("Could not look up Steam player name for " + steamId + ": " + e.getMessage());
        }
        return Optional.empty();
    }
}
