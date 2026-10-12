package com.argus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamUsernameResponse {
    
    @JsonProperty("response")
    private ResponseData response;

    public SteamUsernameResponse() {}

    public ResponseData getResponse() {return response;}
    public void SetResponse(ResponseData response) {this.response = response;}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        
        @JsonProperty("success")
        private int success; // 1 = success, 42 = no match

        @JsonProperty("steamid")
        private String steamId;

        public ResponseData() {}

        public int getSuccess() { return success; }
        public void setSuccess(int success) { this.success = success; }

        public String getSteamId() { return steamId; }
        public void setSteamId(String steamId) { this.steamId = steamId; }
    }
}
