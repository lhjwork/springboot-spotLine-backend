package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.RouteTheme;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RoutePreviewResponse {
    private UUID id;
    private String slug;
    private String title;
    private RouteTheme theme;
    private String area;
    private Integer totalDuration; // minutes
    private Integer totalDistance; // meters
    private Integer spotCount;
    private Integer likesCount;
    private String coverImageUrl;

    public static RoutePreviewResponse from(Route route) {
        return from(route, null);
    }

    public static RoutePreviewResponse from(Route route, String s3BaseUrl) {
        return RoutePreviewResponse.builder()
                .id(route.getId())
                .slug(route.getSlug())
                .title(route.getTitle())
                .theme(route.getTheme())
                .area(route.getArea())
                .totalDuration(route.getTotalDuration())
                .totalDistance(route.getTotalDistance())
                .spotCount(route.getSpots() != null ? route.getSpots().size() : 0)
                .likesCount(route.getLikesCount())
                .coverImageUrl(resolveCoverImageUrl(route, s3BaseUrl))
                .build();
    }

    private static String resolveCoverImageUrl(Route route, String s3BaseUrl) {
        if (s3BaseUrl == null || route.getSpots() == null || route.getSpots().isEmpty()) {
            return null;
        }
        Spot firstSpot = route.getSpots().get(0).getSpot();
        if (firstSpot == null) return null;

        // 1) mediaItems (structured) 우선
        if (firstSpot.getMediaItems() != null && !firstSpot.getMediaItems().isEmpty()) {
            SpotMedia firstMedia = firstSpot.getMediaItems().get(0);
            String key = firstMedia.getThumbnailS3Key() != null
                    ? firstMedia.getThumbnailS3Key()
                    : firstMedia.getS3Key();
            return s3BaseUrl + "/" + key;
        }

        // 2) media (legacy String list) 폴백
        if (firstSpot.getMedia() != null && !firstSpot.getMedia().isEmpty()) {
            return s3BaseUrl + "/" + firstSpot.getMedia().get(0);
        }

        return null;
    }
}
