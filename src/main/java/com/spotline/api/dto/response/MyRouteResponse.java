package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.UserRoute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "내 루트 응답")
@Data
@Builder
public class MyRouteResponse {
    private String id;
    private String routeId;
    private String routeSlug;
    private String title;
    private String area;
    private int spotsCount;
    private String scheduledDate;
    private String status;
    private String completedAt;
    private String parentRouteId;
    private String createdAt;

    public static MyRouteResponse from(UserRoute ur) {
        Route route = ur.getRoute();
        return MyRouteResponse.builder()
            .id(ur.getId().toString())
            .routeId(route.getId().toString())
            .routeSlug(route.getSlug())
            .title(route.getTitle())
            .area(route.getArea())
            .spotsCount(route.getSpots() != null ? route.getSpots().size() : 0)
            .scheduledDate(ur.getScheduledDate())
            .status(ur.getStatus())
            .completedAt(ur.getCompletedAt() != null ? ur.getCompletedAt().toString() : null)
            .parentRouteId(route.getParentRoute() != null ? route.getParentRoute().getId().toString() : route.getId().toString())
            .createdAt(ur.getCreatedAt() != null ? ur.getCreatedAt().toString() : null)
            .build();
    }
}
