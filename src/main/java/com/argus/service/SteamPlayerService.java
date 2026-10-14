package com.argus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class SteamPlayerService {
    
    private static final Logger LOGGER = Logger.getLogger(SteamPlayerService.class.getName());
    private static final String OWNED_GAMES_URL = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&include_appinfo=true&format=json";

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

        try {
            String url = String.format(OWNED_GAMES_URL, apiKey, steamId);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode gamesNode = rootNode.path("response").path("games");

                if (gamesNode.isArray() && !gamesNode.isEmpty()) {
                    List<GameDto> games = objectMapper.readerForListOf(GameDto.class).readValue(gamesNode);
                    return Optional.of(games);
                } else {
                    return Optional.of(Collections.emptyList());
                }
            } else {
                LOGGER.warning("Error http on searching games. Status -> " + response.statusCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.severe("Exception on searching games -> " + e.getMessage());
            return Optional.empty();
        }
    }
}
