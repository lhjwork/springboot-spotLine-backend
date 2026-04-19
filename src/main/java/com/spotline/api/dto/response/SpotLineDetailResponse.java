package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.SpotLineSpot;
import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "SpotLine 상세 응답")
@Data
@Builder
public class SpotLineDetailResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private SpotLineTheme theme;
    private String area;
    private Integer totalDuration;
    private Integer totalDistance;

    private List<SpotLineSpotDetail> spots;

    // Stats
    private Integer likesCount;
    private Integer savesCount;
    private Integer replicationsCount;
    private Integer completionsCount;
    private Integer commentsCount;
    private Integer sharesCount;

    // Creator
    private String creatorType;
    private String creatorName;

    // Variation
    private UUID parentSpotLineId;
    private Integer variationsCount;

    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class SpotLineSpotDetail {
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

    public static SpotLineDetailResponse from(SpotLine spotLine) {
        List<SpotLineSpotDetail> spotDetails = spotLine.getSpots().stream()
                .map(SpotLineDetailResponse::mapSpotLineSpot)
                .toList();

        return SpotLineDetailResponse.builder()
                .id(spotLine.getId())
                .slug(spotLine.getSlug())
                .title(spotLine.getTitle())
                .description(spotLine.getDescription())
                .theme(spotLine.getTheme())
                .area(spotLine.getArea())
                .totalDuration(spotLine.getTotalDuration())
                .totalDistance(spotLine.getTotalDistance())
                .spots(spotDetails)
                .likesCount(spotLine.getLikesCount())
                .savesCount(spotLine.getSavesCount())
                .replicationsCount(spotLine.getReplicationsCount())
                .completionsCount(spotLine.getCompletionsCount())
                .commentsCount(spotLine.getCommentsCount())
                .sharesCount(spotLine.getSharesCount())
                .creatorType(spotLine.getCreatorType())
                .creatorName(spotLine.getCreatorName())
                .parentSpotLineId(spotLine.getParentSpotLine() != null ? spotLine.getParentSpotLine().getId() : null)
                .variationsCount(spotLine.getVariations() != null ? spotLine.getVariations().size() : 0)
                .createdAt(spotLine.getCreatedAt())
                .build();
    }

    private static SpotLineSpotDetail mapSpotLineSpot(SpotLineSpot ss) {
        return SpotLineSpotDetail.builder()
                .order(ss.getSpotOrder())
                .suggestedTime(ss.getSuggestedTime())
                .stayDuration(ss.getStayDuration())
                .walkingTimeToNext(ss.getWalkingTimeToNext())
                .distanceToNext(ss.getDistanceToNext())
                .transitionNote(ss.getTransitionNote())
                .spotId(ss.getSpot().getId())
                .spotSlug(ss.getSpot().getSlug())
                .spotTitle(ss.getSpot().getTitle())
                .spotCategory(ss.getSpot().getCategory().name())
                .spotArea(ss.getSpot().getArea())
                .spotAddress(ss.getSpot().getAddress())
                .spotLatitude(ss.getSpot().getLatitude())
                .spotLongitude(ss.getSpot().getLongitude())
                .crewNote(ss.getSpot().getCrewNote())
                .spotMedia(ss.getSpot().getMedia())
                .build();
    }
}
