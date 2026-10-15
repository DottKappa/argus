package com.argus.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccumulatedGameDto {
    private final int appId;
    private final String name;
    private int maxPlaytime;
    private final List<String> owners = new ArrayList<>();
    private final Set<String> ownerIds = new HashSet<>();

    public AccumulatedGameDto(int appId, String name, int maxPlaytime) {
        this.appId = appId;
        this.name = name;
        this.maxPlaytime = maxPlaytime;
    }

    public void addOwner(String owner) {
        addOwner(owner, owner);
    }

    public void addOwner(String ownerId, String displayName) {
        if (ownerIds.add(ownerId)) {
            owners.add(displayName);
        }
    }

    public void updateMaxPlaytime(int playtime) {
        if (playtime > this.maxPlaytime) {
            this.maxPlaytime = playtime;
        }
    }

    public int getAppId() { return appId; }
    public String getName() { return name; }
    public int getMaxPlaytime() { return maxPlaytime; }
    public List<String> getOwners() { return owners; }
}