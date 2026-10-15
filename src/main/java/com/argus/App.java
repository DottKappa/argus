package com.argus;

import com.argus.controller.UserSearchController;
import com.argus.service.PlayerWorkflowService;
import com.argus.service.SteamApiService;
import com.argus.service.SteamPlayerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.http.HttpClient;

public class App extends Application  {

    @Override 
    public void start(Stage primaryStage) throws Exception 
    {
        HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        String apiKey = System.getenv("STEAM_API_KEY");

        SteamApiService apiService = new SteamApiService(httpClient, apiKey);
        SteamPlayerService playerService = new SteamPlayerService(httpClient, apiKey);
        PlayerWorkflowService workflowService = new PlayerWorkflowService(apiService, playerService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/argus/view/user_search_view.fxml"));
        loader.setControllerFactory(type -> {
            if (type == UserSearchController.class) {
                return new UserSearchController(workflowService);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent root = loader.load();

        primaryStage.setTitle("Argus - Steam Library manager");
        Scene scene = new Scene(root, 1050, 900);
        scene.getStylesheets().add(getClass().getResource("/com/argus/view/user_search.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}
