package com.argus.controller;

import com.argus.service.GameDto;
import com.argus.service.PlayerWorkflowService;
import com.argus.service.SteamApiService;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class UserSearchController {
    
    private static final Logger LOGGER = Logger.getLogger(UserSearchController.class.getName());

    private final PlayerWorkflowService workflowService;

    // UI
    private final StringProperty usernameInput = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    public StringProperty usernameInputProperty() { return usernameInput; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public SimpleListProperty<GameDto> ownedGamesProperty() { return ownedGamesProperty; }

    private final ObservableList<GameDto> ownedGamesList = FXCollections.observableArrayList();
    private final SimpleListProperty<GameDto> ownedGamesProperty = new SimpleListProperty<>(ownedGamesList);

    public UserSearchController(PlayerWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void handleSearchAction()
    {
        String username = usernameInput.get();

        if(username == null || username.isBlank()) {
            statusMessage.set("Inserisci uno username valido");
            return;
        }

        statusMessage.set("Ricerca in corso per: " + username + "...");
        LOGGER.info("Searching steamID for user: " + username);

        try {
            Optional<List<GameDto>> gamesOpt = workflowService.fetchLibraryForUsername(username.trim());
            
            if(gamesOpt.isPresent()) {
                List<GameDto> games = gamesOpt.get();
                ownedGamesList.setAll(games);
                statusMessage.set("Libreria caricata. Giochi trovati: " + games.size());
            } else {
                statusMessage.set("Utente non trovato per username: " + username);
                LOGGER.info("User not found for username: " + username);
            }
        } catch (Exception e) {
            statusMessage.set("Errore durante la chiamata l'esecuzione");
            LOGGER.info("Error on controller: " + e.getMessage());
        }
    }
}
