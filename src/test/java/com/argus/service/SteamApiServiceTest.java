package com.argus.service;

import com.argus.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SteamApiServiceTest {
    
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<String> mockResponse;

    private SteamApiService apiService;
    
    @BeforeEach
    void setUp() 
    {
        apiService = new SteamApiService(mockHttpClient, "MOCK_API_KEY");
    }

    @Test
    @DisplayName("Restituisce lo steamID64 a partire dall'username")
    void returnSteamIdFromUsernameByMock() throws Exception {
        String jsonResponse = """
            {
                "response": {
                    "steamid": "76561197960384218",
                    "success": 1
                }
            }
            """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        
        // Simula Http::fake() intercettando la richiesta HTTP
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        Optional<String> result = apiService.resolveUsernameUrl("gabe");
        assertTrue(result.isPresent());
        assertEquals("76561197960384218", result.get());
        
        // Verifichiamo che la chiamata HTTP sia stata effettuata esattamente una volta
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test 
    @DisplayName("Restituisce Optional.empty() se il vanity username è vuoto o null")
    void returnEmptyWithInvalidName()
    {
        Optional<String> resultNull = apiService.resolveUsernameUrl(null);
        Optional<String> resultEmpty = apiService.resolveUsernameUrl("   ");

        assertTrue(resultNull.isEmpty());
        assertTrue(resultEmpty.isEmpty());
    }

    @Test
    @DisplayName("Restituisce lo SteamID64 se è già essostesso l'id")
    void returnSameIdIfAlreadySteamId()
    {
        String validSteamId = "76561198169235781";
        Optional<String> result = apiService.resolveUsernameUrl(validSteamId);

        assertTrue(result.isPresent());
        assertEquals(validSteamId, result.get());
    }

    @Test
    @DisplayName("Estrae lo SteamID64 da un link profilo diretto")
    void returnSteamIdFromProfileUrl()
    {
        String validSteamId = "76561198169235781";

        Optional<String> result = apiService.resolveUsernameUrl(
                "https://steamcommunity.com/profiles/" + validSteamId + "/");

        assertEquals(Optional.of(validSteamId), result);
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("Estrae lo username vanity da un link personalizzato")
    void extractVanityUsernameFromProfileUrl()
    {
        assertEquals(Optional.of("alice"),
                apiService.extractVanityUsername("https://steamcommunity.com/id/alice/"));
        assertTrue(apiService.extractVanityUsername(
                "https://steamcommunity.com/profiles/76561198365758309").isEmpty());
    }

    @Test
    @DisplayName("Estrae lo username dal link e usa la conversione esistente")
    void resolveUsernameFromProfileUrl() throws Exception {
        String jsonResponse = """
            {
                "response": {
                    "steamid": "76561197960384218",
                    "success": 1
                }
            }
            """;
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Optional<String> result = apiService.resolveUsernameUrl(
                "https://steamcommunity.com/id/gabe/");

        assertEquals(Optional.of("76561197960384218"), result);
        verify(mockHttpClient).send(argThat(request ->
                request.uri().getRawQuery().contains("vanityurl=gabe")),
                any(HttpResponse.BodyHandler.class));
    }

    @Test 
    @DisplayName ("Restituisce Optional.empty() con errore HTTP")
    void returnEmptyOnHttpError() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(500);
        doReturn(errorResponse).when(mockHttpClient).send(any(), any());

        Optional<String> result = apiService.resolveUsernameUrl("gabe");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Restituisce Optional.empty() in caso di eccezione di rete o timeout")
    void returnEmptyOnNetworkException() throws Exception {
        when(mockHttpClient.send(any(), any())).thenThrow(new java.io.IOException("Network down"));

        Optional<String> result = apiService.resolveUsernameUrl("gabe");

        assertTrue(result.isEmpty());
    }
}
