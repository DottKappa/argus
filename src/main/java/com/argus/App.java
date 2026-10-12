package com.argus;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.argus.model.Game;
import com.argus.service.SteamApiService;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Test rapido del Service HTTP
        SteamApiService apiService = new SteamApiService();
        
        // Sostituisci con un tuo SteamID64 o uno pubblico per il test
        String testSteamId = "76561198169235781"; 
        
        List<Game> games = apiService.getOwnedGames(testSteamId);
        System.out.println("--- GIOCHI RECUPERATI: " + games.size() + " ---");
        for (Game game : games.stream().limit(5).toList()) {
            System.out.println("- " + game.getName() + " (AppID: " + game.getAppId() + ")");
            System.out.println("  Icon URL: " + game.getIconFullUrl());
        }

        System.out.flush();

        Label label = new Label("Giochi trovati: " + games.size());
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("Argus");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}