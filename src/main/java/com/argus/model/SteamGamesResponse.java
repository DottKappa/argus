package com.argus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamGamesResponse {

    private ResponseData response;

    public ResponseData getResponse() {return response;}
    public void SetResponse(ResponseData response) {this.response = response;}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {

        @JsonProperty("game_count")
        private int gameCount;
        private List<Game> games;

        public int getGameCount() { return gameCount; }
        public void setGameCount(int gameCount) { this.gameCount = gameCount; }

        public List<Game> getGames() { return games; }
        public void setGames(List<Game> games) { this.games = games; }
    }
    
}
