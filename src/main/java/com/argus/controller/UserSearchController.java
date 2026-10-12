package com.argus.controller;

import com.argus.service.SteamApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Optional;
import java.util.logging.Logger;

public class UserSearchController {
    
    private static final Logger LOGGER = Logger.getLogger(UserSearchController.class.getName());

    private final SteamApiService steamApiService;

    private final StringProperty usernameInput = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty resolvedSteamId = new SimpleStringProperty("");

    public UserSearchController(SteamApiService steamApiService) {
        this.steamApiService = steamApiService;
    }

    // --- Getter per i Binding JavaFX ---
    public StringProperty usernameInputProperty() { return usernameInput; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public StringProperty resolvedSteamIdProperty() { return resolvedSteamId; }

    /**
     * Esegue la logica di business per la risoluzione dello SteamID a partire dall'input utente.
     * Implementa la gestione robusta degli errori e lo stato applicativo (CRUD/Sessione).
     */
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
            Optional<String> steamIdOpt = steamApiService.resolveUsernameUrl(username.trim());

            if(steamIdOpt.isPresent()) {
                String id = steamIdOpt.get();
                resolvedSteamId.set(id);
                statusMessage.set("Utente trovato! SteamID64: " + id);
                LOGGER.info("User found! SteamID64: " + id);

                saveOrUpdateUserSession(username.trim(), id);
            } else {
                resolvedSteamId.set("");
                statusMessage.set("Utente non trovato per username: " + username);
                LOGGER.info("User not found for username: " + username);
            }
        } catch (Exception e) {
            resolvedSteamId.set("");
            statusMessage.set("Errore durante la chiamata API");
            LOGGER.info("Error on API calls: " + e.getMessage());
        }
    }

    /**
     * Simula la logica CRUD di persistenza o aggiornamento della sessione utente
     */
    private void saveOrUpdateUserSession(String username, String steamId) {
        // Qui in futuro collegheremo il repository locale (es. SQLite o file JSON) 
        // per memorizzare lo storico delle ricerche o l'utente attivo.
        // LOGGER.fine("Aggiornato stato sessione locale per: " + username);
    }
}
