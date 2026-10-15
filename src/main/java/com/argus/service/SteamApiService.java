package com.argus.service;

import com.argus.model.Game;
import com.argus.model.SteamGamesResponse;
import com.argus.model.SteamUsernameResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
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
     * Converte uno username o un link profilo Steam in uno SteamID64.
     * @param username Steam username, link profilo o SteamID64
     * @return Optional
     */
    public Optional<String> resolveUsernameUrl(String username)
    {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleaned = username.trim();
        if (cleaned.contains("://")) {
            Optional<String> profileIdentifier = extractProfileIdentifier(cleaned);
            if (profileIdentifier.isEmpty()) {
                return Optional.empty();
            }
            cleaned = profileIdentifier.get();
        }

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
            System.err.println("ERROR: Can't resolve Steam username -> " + e.getMessage());
        }

        return Optional.empty();
    }

    private Optional<String> extractProfileIdentifier(String input)
    {
        Optional<URI> profileUri = parseSteamCommunityUri(input);
        if (profileUri.isEmpty()) {
            return Optional.empty();
        }

        List<String> pathParts = getProfilePathParts(profileUri.get());
        if (pathParts.size() != 2) {
            return Optional.empty();
        }

        String profileType = pathParts.get(0);
        String profileIdentifier = pathParts.get(1);
        if (profileType.equalsIgnoreCase("id") && !profileIdentifier.isBlank()) {
            return Optional.of(profileIdentifier);
        }
        if (profileType.equalsIgnoreCase("profiles")
                && profileIdentifier.matches("^7656119\\d{10}$")) {
            return Optional.of(profileIdentifier);
        }
        return Optional.empty();
    }

    public Optional<String> extractVanityUsername(String profileUrl)
    {
        if (profileUrl == null || profileUrl.isBlank()) {
            return Optional.empty();
        }
        Optional<URI> profileUri = parseSteamCommunityUri(profileUrl);
        if (profileUri.isEmpty()) {
            return Optional.empty();
        }

        List<String> pathParts = getProfilePathParts(profileUri.get());
        if (pathParts.size() == 2 && pathParts.get(0).equalsIgnoreCase("id")) {
            return Optional.of(pathParts.get(1));
        }
        return Optional.empty();
    }

    private Optional<URI> parseSteamCommunityUri(String input)
    {
        try {
            URI uri = URI.create(input);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && host != null
                    && (host.equalsIgnoreCase("steamcommunity.com")
                    || host.equalsIgnoreCase("www.steamcommunity.com"))
                    && uri.getUserInfo() == null) {
                return Optional.of(uri);
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private List<String> getProfilePathParts(URI uri)
    {
        return Arrays.stream(uri.getPath().split("/"))
                .filter(part -> !part.isEmpty())
                .toList();
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
