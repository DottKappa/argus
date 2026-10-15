package com.argus.controller;

import com.argus.service.GamePresentationDto;
import com.argus.service.PlayerWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    @DisplayName("Aggiorna lista e stato quando la ricerca della libreria famiglia ha successo")
    void handleSearchActionSuccess() {
        List<GamePresentationDto> mockGames = List.of(
                new GamePresentationDto(730, "Counter-Strike: Global Offensive", 1200, List.of("gabe"), null),
                new GamePresentationDto(570, "Dota 2", 450, List.of("gabe"), null)
        );

        List<String> profiles = List.of("https://steamcommunity.com/id/gabe");
        when(workflowServiceMock.fetchFamilyLibrary(profiles)).thenReturn(mockGames);

        controller.searchProfiles(profiles);

        assertEquals(2, controller.ownedGamesProperty().size());
        assertEquals("Counter-Strike: Global Offensive", controller.ownedGamesProperty().get(0).name());
        assertEquals("Libreria famiglia caricata. Giochi unici trovati: 2", controller.statusMessageProperty().get());
    }

    @Test
    @DisplayName("Gestisce correttamente i campi vuoti")
    void handleSearchActionEmptyInput() {
        controller.searchProfiles(List.of("   "));

        assertEquals("Inserisci almeno un link o ID Steam valido", controller.statusMessageProperty().get());
        assertEquals(0, controller.ownedGamesProperty().size());
        verifyNoInteractions(workflowServiceMock);
    }

    @Test
    @DisplayName("Gestisce il caso in cui il workflow restituisce una libreria vuota")
    void handleSearchActionNotFound() {
        List<String> profiles = List.of("sconosciuto");
        when(workflowServiceMock.fetchFamilyLibrary(profiles)).thenReturn(List.of());

        controller.searchProfiles(profiles);

        assertEquals(0, controller.ownedGamesProperty().size());
        assertEquals("Nessun gioco trovato. Verifica che i dettagli dei giochi siano pubblici nelle impostazioni privacy di Steam.",
                controller.statusMessageProperty().get());
    }

    @Test
    @DisplayName("Mostra gli errori restituiti dal servizio Steam")
    void handleSearchActionShowsSteamError() {
        List<String> profiles = List.of("https://steamcommunity.com/profiles/76561198365758309");
        when(workflowServiceMock.fetchFamilyLibrary(profiles))
                .thenThrow(new IllegalStateException("Steam GetOwnedGames ha risposto HTTP 403"));

        controller.searchProfiles(profiles);

        assertEquals("Errore durante la ricerca: Steam GetOwnedGames ha risposto HTTP 403",
                controller.statusMessageProperty().get());
    }

    @Test
    @DisplayName("Combina filtro utente, ore giocate e ricerca testuale senza distinzione maiuscole")
    void filtersGamesByOwnerPlaytimeAndName() {
        List<GamePresentationDto> games = List.of(
                new GamePresentationDto(1, "Portal", 120, List.of("Alice"), null),
                new GamePresentationDto(2, "Portal 2", 0, List.of("Alice"), null),
                new GamePresentationDto(3, "Half-Life", 60, List.of("Bob"), null)
        );

        List<GamePresentationDto> filtered = UserSearchController.filterGames(
                games, "Alice", true, "PORT"
        );

        assertEquals(List.of(1), filtered.stream().map(GamePresentationDto::appId).toList());
    }

    @Test
    @DisplayName("La ricerca vuota non filtra e il filtro ore esclude i giochi non avviati")
    void filtersUnplayedGames() {
        List<GamePresentationDto> games = List.of(
                new GamePresentationDto(1, "Portal", 120, List.of("Alice"), null),
                new GamePresentationDto(2, "Portal 2", 0, List.of("Alice"), null)
        );

        assertEquals(List.of(1, 2), UserSearchController.filterGames(games, "Tutti gli utenti", false, "")
                .stream().map(GamePresentationDto::appId).toList());
        assertEquals(List.of(1), UserSearchController.filterGames(games, "Tutti gli utenti", true, " ")
                .stream().map(GamePresentationDto::appId).toList());
    }

}