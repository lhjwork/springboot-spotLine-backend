package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecommendedSpotResponse {
    private UUID id;
    private String slug;
    private String title;
    private SpotCategory category;
    private String crewNote;
    private String address;
    private String area;
    private Double latitude;
    private Double longitude;
    private List<String> tags;
    private String thumbnailUrl;
    private Integer viewsCount;
    private Integer likesCount;
    private TimeOfDay bestTimeOfDay;
    private WeatherCondition bestWeatherCondition;
    private Boolean isIndoor;
    private double contextScore;

    public static RecommendedSpotResponse from(Spot spot, double contextScore, String s3BaseUrl) {
        String thumbnail = null;
        if (s3BaseUrl != null && spot.getMediaItems() != null && !spot.getMediaItems().isEmpty()) {
            thumbnail = s3BaseUrl + "/" + spot.getMediaItems().get(0).getS3Key();
        } else if (spot.getMedia() != null && !spot.getMedia().isEmpty()) {
            thumbnail = spot.getMedia().get(0);
        }

        return RecommendedSpotResponse.builder()
                .id(spot.getId())
                .slug(spot.getSlug())
                .title(spot.getTitle())
                .category(spot.getCategory())
                .crewNote(spot.getCrewNote())
                .address(spot.getAddress())
                .area(spot.getArea())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .tags(spot.getTags())
                .thumbnailUrl(thumbnail)
                .viewsCount(spot.getViewsCount())
                .likesCount(spot.getLikesCount())
                .bestTimeOfDay(spot.getBestTimeOfDay())
                .bestWeatherCondition(spot.getBestWeatherCondition())
                .isIndoor(spot.getIsIndoor())
                .contextScore(contextScore)
                .build();
    }
}
