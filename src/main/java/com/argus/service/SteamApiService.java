package com.argus.service;

import com.argus.model.Game;
import com.argus.model.SteamGamesResponse;
import com.argus.model.SteamUsernameResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SteamApiService {
    
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SteamApiService() {
        this(HttpClient.newHttpClient(), System.getenv("STEAM_API_KEY"));
    }

    public SteamApiService(HttpClient httpClient, String apiKey)
    {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Converte un vanity username in un steamID64
     * @param steamId
     * @return Optional
     */
    public Optional<String> resolveUsernameUrl(String username)
    {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleaned = username.trim();

        // Se è già steamID64 lo ritorno senza fare niente
        if (cleaned.matches("^7656119\\d{10}$")) {
            return Optional.of(cleaned);
        }

        if (apiKey == null || apiKey.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(
            "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s",
            apiKey,
            cleaned
        );

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                SteamUsernameResponse result = objectMapper.readValue(response.body(), SteamUsernameResponse.class);
                if (result.getResponse() != null && result.getResponse().getSuccess() == 1) {
                    return Optional.ofNullable(result.getResponse().getSteamId());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Can't find vanity username url -> " + e.getMessage());            
        }

        return Optional.empty();
    }

    /**
     * Recupera la lista dei giochi da SteamID64
     */
    public List<Game> getOwnedGames(String steamId)
    {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: STEAM_API_KEY not found in .env");
            return Collections.emptyList();
        }

        String url = String.format(
            "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&format=json&include_appinfo=true&include_played_free_games=true",
            apiKey, 
            steamId
        );

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200) {
                // Mappiamo json sul DTO
                SteamGamesResponse apiResult = objectMapper.readValue(response.body(), SteamGamesResponse.class);
                if(apiResult.getResponse() != null && apiResult.getResponse().getGames() != null) {
                    return apiResult.getResponse().getGames();
                }
            } else {
                System.err.println("ERROR: Api steam error -> " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("EXCEPTION: Api steam error -> " + e.getMessage());
        }

        return Collections.emptyList();
    }
}
