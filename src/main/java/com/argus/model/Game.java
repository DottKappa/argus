package com.argus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {

    @JsonProperty("appid")
    private long appId;

    private String name;

    @JsonProperty("playtime_forever")
    private int playtimeForever;

    @JsonProperty("img_icon_url")
    private String imgIconUrl;

    // GET - SET
    public long getAppId() {return appId;}
    public void settAppId(long appId) {this.appId = appId;}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPlaytimeForever() { return playtimeForever; }
    public void setPlaytimeForever(int playtimeForever) { this.playtimeForever = playtimeForever; }

    public String getImgIconUrl() { return imgIconUrl; }
    public void setImgIconUrl(String imgIconUrl) { this.imgIconUrl = imgIconUrl; }

    /**
     * Costruisco url icona
     */
    public String getIconFullUrl() 
    {
        if (imgIconUrl == null || imgIconUrl.isEmpty()) {
            return null;
        }
        return String.format("http://media.steampowered.com/steamcommunity/public/images/apps/%d/%s.jpg", appId, imgIconUrl);
    }
}
