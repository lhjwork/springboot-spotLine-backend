package com.spotline.api.dto.response;

import com.spotline.api.infrastructure.place.PlaceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "QR Discovery 응답")
@Data
@Builder
public class DiscoverResponse {

    private CurrentSpotInfo currentSpot;
    private NextSpotInfo nextSpot;
    private List<SpotDetailResponse> nearbySpots;
    private List<SpotLinePreviewResponse> popularSpotLines;
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
