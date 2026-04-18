package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserSpotLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "내 SpotLine 응답")
@Data
@Builder
public class MySpotLineResponse {
    private String id;
    private String spotLineId;
    private String spotLineSlug;
    private String title;
    private String area;
    private int spotsCount;
    private String scheduledDate;
    private String status;
    private String completedAt;
    private String parentSpotLineId;
    private String createdAt;
    private List<SpotSummary> spots;

    @Data
    @Builder
    public static class SpotSummary {
        private String id;
        private String title;
        private String category;
        private String address;
        private int order;
    }

    public static MySpotLineResponse from(UserSpotLine ur) {
        SpotLine spotLine = ur.getSpotLine();
        List<SpotSummary> spotSummaries = spotLine.getSpots() != null
            ? spotLine.getSpots().stream()
                .map(sls -> SpotSummary.builder()
                    .id(sls.getSpot().getId().toString())
                    .title(sls.getSpot().getTitle())
                    .category(sls.getSpot().getCategory() != null ? sls.getSpot().getCategory().name() : null)
                    .address(sls.getSpot().getAddress())
                    .order(sls.getSpotOrder())
                    .build())
                .toList()
            : List.of();

        return MySpotLineResponse.builder()
            .id(ur.getId().toString())
            .spotLineId(spotLine.getId().toString())
            .spotLineSlug(spotLine.getSlug())
            .title(spotLine.getTitle())
            .area(spotLine.getArea())
            .spotsCount(spotSummaries.size())
            .scheduledDate(ur.getScheduledDate())
            .status(ur.getStatus())
            .completedAt(ur.getCompletedAt() != null ? ur.getCompletedAt().toString() : null)
            .parentSpotLineId(spotLine.getParentSpotLine() != null ? spotLine.getParentSpotLine().getId().toString() : spotLine.getId().toString())
            .createdAt(ur.getCreatedAt() != null ? ur.getCreatedAt().toString() : null)
            .spots(spotSummaries)
            .build();
    }
}
