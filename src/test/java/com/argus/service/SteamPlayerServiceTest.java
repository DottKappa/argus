package com.argus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.bytebuddy.agent.builder.AgentBuilder.CircularityLock.Global;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SteamPlayerServiceTest {
    
    private HttpClient mockHttpClient;
    private HttpResponse<Object> mockResponse;
    private SteamPlayerService playerService;

    @BeforeEach 
    void setUp()
    {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);
        playerService = new SteamPlayerService(mockHttpClient, "FAKE_API_KEY");
    }

    @Test 
    @DisplayName("Restituisce la lista dei giochi")
    void returnOwnedGames() throws Exception {
        String jsonResponse = """
            {
                "response": {
                    "game_count": 2,
                    "games": [
                        { "appid": 730, "name": "Counter-Strike: Global Offensive", "playtime_forever": 1200 },
                        { "appid": 570, "name": "Dota 2", "playtime_forever": 450 }
                    ]
                }
            }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        doReturn(mockResponse).when(mockHttpClient).send(any(), any());

        Optional<List<GameDto>> gamesOpt = playerService.getOwnedGames("76561197960384218");

        assertTrue(gamesOpt.isPresent());
        assertEquals(2, gamesOpt.get().size());
        assertEquals("Counter-Strike: Global Offensive", gamesOpt.get().get(0).getName());
    }
}
