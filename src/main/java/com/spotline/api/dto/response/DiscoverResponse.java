package com.spotline.api.dto.response;

import com.spotline.api.infrastructure.place.PlaceInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DiscoverResponse {

    private CurrentSpotInfo currentSpot;
    private NextSpotInfo nextSpot;
    private List<SpotDetailResponse> nearbySpots;
    private List<RoutePreviewResponse> popularRoutes;
    private String area;
    private boolean locationGranted;

    @Data
    @Builder
    public static class CurrentSpotInfo {
        private SpotDetailResponse spot;
        private PlaceInfo placeInfo;
        private double distanceFromUser; // meters
    }

    @Data
    @Builder
    public static class NextSpotInfo {
        private SpotDetailResponse spot;
        private PlaceInfo placeInfo;
        private double distanceFromCurrent; // meters
        private int walkingTime; // minutes
    }
}
