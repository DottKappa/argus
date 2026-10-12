package com.argus.controller;

import com.argus.service.SteamApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserSearchControllerTest {
    
    private SteamApiService steamApiServiceMock;
    private UserSearchController controller;

    @BeforeEach 
    void setUp() 
    {
        steamApiServiceMock = mock(SteamApiService.class);
        controller = new UserSearchController(steamApiServiceMock);
    }

    @Test 
    @DisplayName("Aggiorna le proprierty se l'utente viene trovato")
    void handleSearchActionSuccess() throws Exception 
    {
        when(steamApiServiceMock.resolveUsernameUrl("gabe")).thenReturn(Optional.of("76561197960384218"));

        controller.usernameInputProperty().set("gabe");
        controller.handleSearchAction();

        assertEquals("76561197960384218", controller.resolvedSteamIdProperty().get());
        assertEquals("Utente trovato! SteamID64: 76561197960384218", controller.statusMessageProperty().get());
    }

    @Test 
    @DisplayName("Gestisce l'input vuoto o nullo")
    void handleSearchActionEmptyInput()
    {
        controller.usernameInputProperty().set("   ");
        controller.handleSearchAction();

        assertEquals("Inserisci uno username valido", controller.statusMessageProperty().get());
        assertEquals("", controller.resolvedSteamIdProperty().get());
        verifyNoInteractions(steamApiServiceMock);
    }

    @Test
    @DisplayName("Gestisce il caso in cui l'utente Steam non esiste")
    void handleSearchActionNotFound() throws Exception {
        when(steamApiServiceMock.resolveUsernameUrl("sconosciuto")).thenReturn(Optional.empty());

        controller.usernameInputProperty().set("sconosciuto");
        controller.handleSearchAction();

        assertEquals("", controller.resolvedSteamIdProperty().get());
        assertEquals("Utente non trovato per username: sconosciuto", controller.statusMessageProperty().get());
    }
}
