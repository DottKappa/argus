package com.argus.controller;

import com.argus.service.GameDto;
import com.argus.service.PlayerWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserSearchControllerTest {

    private PlayerWorkflowService workflowServiceMock;
    private UserSearchController controller;

    @BeforeEach
    void setUp() {
        workflowServiceMock = mock(PlayerWorkflowService.class);
        controller = new UserSearchController(workflowServiceMock);
    }

    @Test
    @DisplayName("Aggiorna con successo le property e la lista giochi quando il workflow ha successo")
    void handleSearchActionSuccess() {
        List<GameDto> mockGames = List.of(
                new GameDto(730, "Counter-Strike: Global Offensive", 1200),
                new GameDto(570, "Dota 2", 450)
        );

        when(workflowServiceMock.fetchLibraryForUsername("gabe")).thenReturn(Optional.of(mockGames));

        controller.usernameInputProperty().set("gabe");
        controller.handleSearchAction();

        assertEquals(2, controller.ownedGamesProperty().size());
        assertEquals("Counter-Strike: Global Offensive", controller.ownedGamesProperty().get(0).getName());
        assertEquals("Libreria caricata con successo! Giochi trovati: 2", controller.statusMessageProperty().get());
    }

    @Test
    @DisplayName("Gestisce correttamente l'input vuoto o nullo")
    void handleSearchActionEmptyInput() {
        controller.usernameInputProperty().set("   ");
        controller.handleSearchAction();

        assertEquals("Inserisci uno username valido.", controller.statusMessageProperty().get());
        assertEquals(0, controller.ownedGamesProperty().size());
        verifyNoInteractions(workflowServiceMock);
    }

    @Test
    @DisplayName("Gestisce il caso in cui il workflow non trova l'utente o la libreria")
    void handleSearchActionNotFound() {
        when(workflowServiceMock.fetchLibraryForUsername("sconosciuto")).thenReturn(Optional.empty());

        controller.usernameInputProperty().set("sconosciuto");
        controller.handleSearchAction();

        assertEquals(0, controller.ownedGamesProperty().size());
        assertEquals("Utente non trovato o libreria non accessibile.", controller.statusMessageProperty().get());
    }
}