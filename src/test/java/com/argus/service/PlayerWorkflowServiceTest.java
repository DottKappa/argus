package com.argus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class PlayerWorkflowServiceTest {
    
    private SteamApiService steamApiServiceMock;
    private SteamPlayerService steamPlayerServiceMock;
    private PlayerWorkflowService playerWorkflowService;

    @BeforeEach 
    void setUp() 
    {
        steamApiServiceMock = Mockito.mock(SteamApiService.class);
        steamPlayerServiceMock = Mockito.mock(SteamPlayerService.class);
        playerWorkflowService = new PlayerWorkflowService(steamApiServiceMock, steamPlayerServiceMock);
    }

    @Test 
    void testFetchFamilyLibrary()
    {
        String user1 = "alice";
        String user2 = "bob";

        when(steamApiServiceMock.resolveUsernameUrl(user1)).thenReturn(Optional.of("ID_ALICE"));
        when(steamApiServiceMock.resolveUsernameUrl(user2)).thenReturn(Optional.of("ID_BOB"));

        List<GameDto> aliceGames = List.of(
            new GameDto(10, "Cyberpunk2077", 100),
            new GameDto(20, "Call of duty", 50)
        );
        when(steamPlayerServiceMock.getOwnedGames("ID_ALICE")).thenReturn(Optional.of(aliceGames));
        List<GameDto> bobGames = List.of(
            new GameDto(20, "Call of duty", 200),
            new GameDto(30, "Counter strike 2", 10)
        );
        when(steamPlayerServiceMock.getOwnedGames("ID_BOB")).thenReturn(Optional.of(bobGames));

        List<GamePresentationDto> familyLibrary = playerWorkflowService.fetchFamilyLibrary(List.of(user1, user2));

        assertNotNull(familyLibrary);
        assertEquals(3, familyLibrary.size(), "Dovrebbero esserci 3 giochi unici in totale (10, 20, 30)");

        GamePresentationDto sharedGame = familyLibrary.stream()
                .filter(g -> g.appId() == 20)
                .findFirst()
                .orElseThrow();

        assertEquals("Call of duty", sharedGame.name());
        assertEquals(200, sharedGame.playtimeForever(), "Il playtime massimo deve essere 200");
        assertEquals(2, sharedGame.getLicenseCount(), "Il gioco deve avere 2 licenze in famiglia");
        assertTrue(sharedGame.owners().contains("alice"));
        assertTrue(sharedGame.owners().contains("bob"));
    }

    @Test
    void testFetchFamilyLibrary_EmptyList() {
        List<GamePresentationDto> result = playerWorkflowService.fetchFamilyLibrary(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void vanityProfileLinkShowsOnlyVanityUsernameAsOwner() {
        String profileLink = "https://steamcommunity.com/id/alice/";
        when(steamApiServiceMock.resolveUsernameUrl(profileLink)).thenReturn(Optional.of("ID_ALICE"));
        when(steamApiServiceMock.extractVanityUsername(profileLink)).thenReturn(Optional.of("alice"));
        when(steamPlayerServiceMock.getOwnedGames("ID_ALICE"))
                .thenReturn(Optional.of(List.of(new GameDto(10, "Game", 60))));

        List<GamePresentationDto> games = playerWorkflowService.fetchFamilyLibrary(List.of(profileLink));

        assertEquals(List.of("alice"), games.get(0).owners());
    }

    @Test
    void numericProfileLinkLooksUpPlayerNameForOwner() {
        String profileLink = "https://steamcommunity.com/profiles/76561198365758309";
        when(steamApiServiceMock.resolveUsernameUrl(profileLink))
                .thenReturn(Optional.of("76561198365758309"));
        when(steamPlayerServiceMock.getOwnedGames("76561198365758309"))
                .thenReturn(Optional.of(List.of(new GameDto(10, "Game", 60))));
        when(steamPlayerServiceMock.getPlayerName("76561198365758309"))
                .thenReturn(Optional.of("Alice Persona"));

        List<GamePresentationDto> games = playerWorkflowService.fetchFamilyLibrary(List.of(profileLink));

        assertEquals(List.of("Alice Persona"), games.get(0).owners());
    }

    @Test
    void countsDifferentSteamAccountsEvenWhenTheySharePersonaName() {
        String firstProfile = "https://steamcommunity.com/profiles/76561198365758309";
        String secondProfile = "https://steamcommunity.com/profiles/76561198365758310";
        when(steamApiServiceMock.resolveUsernameUrl(firstProfile)).thenReturn(Optional.of("ID_ALICE"));
        when(steamApiServiceMock.resolveUsernameUrl(secondProfile)).thenReturn(Optional.of("ID_BOB"));
        when(steamPlayerServiceMock.getOwnedGames("ID_ALICE"))
                .thenReturn(Optional.of(List.of(new GameDto(10, "Game", 60))));
        when(steamPlayerServiceMock.getOwnedGames("ID_BOB"))
                .thenReturn(Optional.of(List.of(new GameDto(10, "Game", 30))));
        when(steamPlayerServiceMock.getPlayerName("ID_ALICE")).thenReturn(Optional.of("Player"));
        when(steamPlayerServiceMock.getPlayerName("ID_BOB")).thenReturn(Optional.of("Player"));

        GamePresentationDto game = playerWorkflowService.fetchFamilyLibrary(List.of(firstProfile, secondProfile))
                .get(0);

        assertEquals(2, game.getLicenseCount());
        assertEquals(List.of("Player", "Player"), game.owners());
    }
}
