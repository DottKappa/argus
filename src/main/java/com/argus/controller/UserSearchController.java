package com.argus.controller;

import com.argus.service.GamePresentationDto;
import com.argus.service.PlayerWorkflowService;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UserSearchController {
    
    private static final Logger LOGGER = Logger.getLogger(UserSearchController.class.getName());

    private final PlayerWorkflowService workflowService;

    // UI
    @FXML private VBox rootContainer;
    @FXML private VBox inputFieldsContainer;
    @FXML private Label statusLabel;
    @FXML private GridPane gamesGrid;
    @FXML private ScrollPane gamesScrollPane;
    @FXML private ComboBox<String> ownerFilter;
    @FXML private CheckBox playedOnlyFilter;
    @FXML private TextField gameSearchField;
    @FXML private VBox gameDetailsPanel;
    @FXML private Button themeToggleButton;

    private final List<TextField> profileInputFields = new ArrayList<>();
    private final List<HBox> profileInputRows = new ArrayList<>();
    private final Map<Integer, Image> coverCache = new HashMap<>();
    private GamePresentationDto selectedGame;
    private boolean darkTheme;
    private static final String ALL_USERS = "Tutti gli utenti";

    private final StringProperty statusMessage = new SimpleStringProperty("");
    public StringProperty statusMessageProperty() { return statusMessage; }

    private final ObservableList<GamePresentationDto> ownedGamesList = FXCollections.observableArrayList();
    private final SimpleListProperty<GamePresentationDto> ownedGamesProperty = new SimpleListProperty<>(ownedGamesList);
    public SimpleListProperty<GamePresentationDto> ownedGamesProperty() { return ownedGamesProperty; }

    public UserSearchController(PlayerWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public UserSearchController() { this.workflowService = null; }

    @FXML 
    public void initialize() {
        if (rootContainer != null && !rootContainer.getStyleClass().contains("light-theme")) {
            rootContainer.getStyleClass().add("light-theme");
        }
        if (gamesScrollPane != null) {
            VBox.setVgrow(gamesScrollPane, Priority.ALWAYS);
        }
        if (gamesGrid != null) {
            gamesGrid.setAlignment(Pos.TOP_CENTER);
        }
        if (ownerFilter != null) {
            ownerFilter.valueProperty().addListener((observable, oldValue, newValue) -> renderGamesGrid());
        }
        if (playedOnlyFilter != null) {
            playedOnlyFilter.selectedProperty().addListener((observable, oldValue, newValue) -> renderGamesGrid());
        }
        if (gameSearchField != null) {
            gameSearchField.textProperty().addListener((observable, oldValue, newValue) -> renderGamesGrid());
        }
        if (gameDetailsPanel != null) {
            gameDetailsPanel.setVisible(false);
            gameDetailsPanel.setManaged(false);
        }
        if (statusLabel != null) {
            statusLabel.textProperty().bind(statusMessage);
        }

        // Aggiunge un primo campo di input all'avvio
        if (inputFieldsContainer != null) {
            addProfileInputField("");
        }
    }

    private void addProfileInputField(String initialValue) {
        TextField textField = new TextField(initialValue);
        textField.setPromptText("Link profilo Steam o SteamID64");
        HBox.setHgrow(textField, Priority.ALWAYS);

        Button addButton = new Button("+");
        addButton.setAccessibleText("Aggiungi utente");
        addButton.setOnAction(event -> addProfileInputField(""));

        Button removeButton = new Button("-");
        removeButton.setAccessibleText("Rimuovi utente");
        removeButton.setOnAction(event -> removeProfileInputField(textField));

        HBox row = new HBox(8, textField, addButton, removeButton);
        row.setPadding(new Insets(2, 2, 2, 2));
        profileInputFields.add(textField);
        profileInputRows.add(row);
        inputFieldsContainer.getChildren().add(row);
        updateRemoveButtons();
    }

    private void removeProfileInputField(TextField textField) {
        if (profileInputFields.size() <= 1) {
            return;
        }
        int index = profileInputFields.indexOf(textField);
        if (index < 0) {
            return;
        }
        profileInputFields.remove(index);
        inputFieldsContainer.getChildren().remove(profileInputRows.remove(index));
        updateRemoveButtons();
    }

    private void updateRemoveButtons() {
        boolean onlyOneRow = profileInputFields.size() == 1;
        for (HBox row : profileInputRows) {
            ((Button) row.getChildren().get(2)).setDisable(onlyOneRow);
        }
    }

    @FXML 
    public void handleSearchAction() {
        List<String> profileIdentifiers = profileInputFields.stream()
                .map(TextField::getText)
                .toList();
        searchProfiles(profileIdentifiers);
    }

    void searchProfiles(List<String> identifiers) {
        List<String> profileIdentifiers = identifiers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (profileIdentifiers.isEmpty()) {
            statusMessage.set("Inserisci almeno un link o ID Steam valido");
            return;
        }

        statusMessage.set("Elaborazione libreria famiglia per " + profileIdentifiers.size() + " account...");
        LOGGER.info("Fetching family library for profiles: " + profileIdentifiers);

        try {
            List<GamePresentationDto> familyGames = workflowService.fetchFamilyLibrary(profileIdentifiers);
            
            if(!familyGames.isEmpty()) {
                ownedGamesList.setAll(familyGames);
                selectedGame = null;
                updateOwnerFilterOptions();
                renderGameDetails();
                renderGamesGrid();
                statusMessage.set("Libreria famiglia caricata. Giochi unici trovati: " + familyGames.size());
            } else {
                statusMessage.set("Nessun gioco trovato. Verifica che i dettagli dei giochi siano pubblici nelle impostazioni privacy di Steam.");
                LOGGER.info("No games found. Steam game details may be private.");
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? "errore non specificato" : e.getMessage();
            statusMessage.set("Errore durante la ricerca: " + message);
            LOGGER.severe("Error on controller: " + message);
        }
    }

    private void updateOwnerFilterOptions() {
        if (ownerFilter == null) {
            return;
        }
        String previousSelection = ownerFilter.getValue();
        List<String> owners = ownedGamesList.stream()
                .flatMap(game -> game.owners() == null ? java.util.stream.Stream.empty() : game.owners().stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        ownerFilter.getItems().setAll(ALL_USERS);
        ownerFilter.getItems().addAll(owners);
        ownerFilter.setValue(owners.contains(previousSelection) ? previousSelection : ALL_USERS);
    }

    private void renderGamesGrid() {
        if (gamesGrid == null) {
            return;
        }
        gamesGrid.getChildren().clear();
        List<GamePresentationDto> visibleGames = filterGames(
                ownedGamesList,
                ownerFilter == null ? null : ownerFilter.getValue(),
                playedOnlyFilter != null && playedOnlyFilter.isSelected(),
                gameSearchField == null ? "" : gameSearchField.getText()
        );
        if (selectedGame != null && visibleGames.stream().noneMatch(game -> game.appId() == selectedGame.appId())) {
            selectedGame = null;
            renderGameDetails();
        }

        for (int index = 0; index < visibleGames.size(); index++) {
            gamesGrid.add(createGameCard(visibleGames.get(index)), index % 5, index / 5);
        }
    }

    static List<GamePresentationDto> filterGames(List<GamePresentationDto> games, String owner,
                                                  boolean playedOnly, String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        return games.stream()
                .filter(game -> owner == null || owner.isBlank() || ALL_USERS.equals(owner)
                        || (game.owners() != null && game.owners().contains(owner)))
                .filter(game -> !playedOnly || game.playtimeForever() > 0)
                .filter(game -> query.isEmpty()
                        || (game.name() != null && game.name().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    private VBox createGameCard(GamePresentationDto game) {
        ImageView cover = new ImageView();
        cover.setFitWidth(160);
        cover.setFitHeight(75);
        cover.setPreserveRatio(true);

        Label fallback = new Label("Copertina non disponibile");
        fallback.setWrapText(true);
        StackPane imageContainer = new StackPane(fallback, cover);
        imageContainer.setMinHeight(80);
        imageContainer.setPrefWidth(170);

        String imageUrl = game.headerImageUrl() == null || game.headerImageUrl().isBlank()
                ? "https://cdn.cloudflare.steamstatic.com/steam/apps/" + game.appId() + "/header.jpg"
                : game.headerImageUrl();
        Image image = coverCache.computeIfAbsent(game.appId(),
                appId -> new Image(imageUrl, 160, 75, true, true, true));
        cover.setImage(image);
        cover.setVisible(false);
        image.errorProperty().addListener((observable, wasError, hasError) -> {
            cover.setVisible(!hasError);
            fallback.setVisible(hasError);
        });
        if (image.isError()) {
            fallback.setVisible(true);
        } else {
            cover.setVisible(true);
            fallback.setVisible(false);
        }

        Label name = new Label(game.name());
        name.setWrapText(true);
        name.setMaxWidth(170);
        name.setAlignment(Pos.CENTER);

        VBox card = new VBox(6, imageContainer, name);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setPrefWidth(180);
        card.setMinHeight(125);
        card.setCursor(Cursor.HAND);
        card.setFocusTraversable(true);
        card.setAccessibleText(game.name() + ", apri i dettagli");
        card.getStyleClass().add("game-card");
        if (selectedGame != null && selectedGame.appId() == game.appId()) {
            card.getStyleClass().add("selected-game-card");
        }
        card.setOnMouseClicked(event -> selectGame(game));
        card.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER
                    || event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                selectGame(game);
            }
        });
        return card;
    }

    private void selectGame(GamePresentationDto game) {
        selectedGame = selectedGame != null && selectedGame.appId() == game.appId() ? null : game;
        renderGameDetails();
        renderGamesGrid();
    }

    private void renderGameDetails() {
        if (gameDetailsPanel == null) {
            return;
        }
        gameDetailsPanel.getChildren().clear();
        boolean hasSelection = selectedGame != null;
        gameDetailsPanel.setVisible(hasSelection);
        gameDetailsPanel.setManaged(hasSelection);
        if (!hasSelection) {
            return;
        }

        Label name = new Label(selectedGame.name());
        name.getStyleClass().add("details-title");
        Label playtime = new Label(String.format("Ore: %.1f", selectedGame.playtimeForever() / 60.0));
        Label licenses = new Label("Licenze: " + selectedGame.getLicenseCount());
        Label owners = new Label("Utenti: " + (selectedGame.owners() == null
                ? "" : String.join(", ", selectedGame.owners())));
        owners.setWrapText(true);
        Button closeButton = new Button("Chiudi");
        closeButton.setOnAction(event -> selectGame(selectedGame));
        HBox heading = new HBox(12, name, closeButton);
        heading.setAlignment(Pos.CENTER_LEFT);
        gameDetailsPanel.getChildren().addAll(heading, playtime, licenses, owners);
    }

    @FXML
    public void handleToggleThemeAction() {
        darkTheme = !darkTheme;
        if (rootContainer != null) {
            rootContainer.getStyleClass().remove("light-theme");
            rootContainer.getStyleClass().remove("dark-theme");
            rootContainer.getStyleClass().add(darkTheme ? "dark-theme" : "light-theme");
        }
        if (themeToggleButton != null) {
            themeToggleButton.setText(darkTheme ? "Tema chiaro" : "Tema scuro");
        }
    }
}