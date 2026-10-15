package com.argus.service;

import java.util.List;

public record GamePresentationDto(
    int appId,
    String name,
    int playtimeForever,
    List<String> owners,
    String headerImageUrl
) {
    public int getLicenseCount() {
        return owners != null ? owners.size() : 0;
    }
}