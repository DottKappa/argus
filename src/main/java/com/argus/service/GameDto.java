package com.argus.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameDto (
    @JsonProperty("appid") int appId,
    @JsonProperty("name") String name,
    @JsonProperty("playtime_forever") int playtimeForever
) {
    public String getName() 
    {
        return name;
    }

    public int getAppId() 
    {
        return appId;
    }
}
