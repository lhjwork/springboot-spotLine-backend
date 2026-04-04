package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.RouteSpot;
import com.spotline.api.domain.enums.RouteTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "루트 상세 응답")
@Data
@Builder
public class RouteDetailResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private RouteTheme theme;
    private String area;
    private Integer totalDuration;
    private Integer totalDistance;

    private List<RouteSpotDetail> spots;

    // Stats
    private Integer likesCount;
    private Integer savesCount;
    private Integer replicationsCount;
    private Integer completionsCount;
    private Integer commentsCount;

    // Creator
    private String creatorType;
    private String creatorName;

    // Variation
    private UUID parentRouteId;
    private Integer variationsCount;

    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class RouteSpotDetail {
        private Integer order;
        private String suggestedTime;
        private Integer stayDuration;
        private Integer walkingTimeToNext;
        private Integer distanceToNext;
        private String transitionNote;

        // Spot summary
        private UUID spotId;
        private String spotSlug;
        private String spotTitle;
        private String spotCategory;
        private String spotArea;
        private String spotAddress;
        private Double spotLatitude;
        private Double spotLongitude;
        private String crewNote;
        private List<String> spotMedia;
    }

    public static RouteDetailResponse from(Route route) {
        List<RouteSpotDetail> spotDetails = route.getSpots().stream()
                .map(RouteDetailResponse::mapRouteSpot)
                .toList();

        return RouteDetailResponse.builder()
                .id(route.getId())
                .slug(route.getSlug())
                .title(route.getTitle())
                .description(route.getDescription())
                .theme(route.getTheme())
                .area(route.getArea())
                .totalDuration(route.getTotalDuration())
                .totalDistance(route.getTotalDistance())
                .spots(spotDetails)
                .likesCount(route.getLikesCount())
                .savesCount(route.getSavesCount())
                .replicationsCount(route.getReplicationsCount())
                .completionsCount(route.getCompletionsCount())
                .commentsCount(route.getCommentsCount())
                .creatorType(route.getCreatorType())
                .creatorName(route.getCreatorName())
                .parentRouteId(route.getParentRoute() != null ? route.getParentRoute().getId() : null)
                .variationsCount(route.getVariations() != null ? route.getVariations().size() : 0)
                .createdAt(route.getCreatedAt())
                .build();
    }

    private static RouteSpotDetail mapRouteSpot(RouteSpot rs) {
        return RouteSpotDetail.builder()
                .order(rs.getSpotOrder())
                .suggestedTime(rs.getSuggestedTime())
                .stayDuration(rs.getStayDuration())
                .walkingTimeToNext(rs.getWalkingTimeToNext())
                .distanceToNext(rs.getDistanceToNext())
                .transitionNote(rs.getTransitionNote())
                .spotId(rs.getSpot().getId())
                .spotSlug(rs.getSpot().getSlug())
                .spotTitle(rs.getSpot().getTitle())
                .spotCategory(rs.getSpot().getCategory().name())
                .spotArea(rs.getSpot().getArea())
                .spotAddress(rs.getSpot().getAddress())
                .spotLatitude(rs.getSpot().getLatitude())
                .spotLongitude(rs.getSpot().getLongitude())
                .crewNote(rs.getSpot().getCrewNote())
                .spotMedia(rs.getSpot().getMedia())
                .build();
    }
}
