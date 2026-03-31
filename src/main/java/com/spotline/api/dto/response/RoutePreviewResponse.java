package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
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

    public static RoutePreviewResponse from(Route route) {
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
                .build();
    }
}
