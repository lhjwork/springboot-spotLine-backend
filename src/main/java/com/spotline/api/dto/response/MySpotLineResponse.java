package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserSpotLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

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

    public static MySpotLineResponse from(UserSpotLine ur) {
        SpotLine spotLine = ur.getSpotLine();
        return MySpotLineResponse.builder()
            .id(ur.getId().toString())
            .spotLineId(spotLine.getId().toString())
            .spotLineSlug(spotLine.getSlug())
            .title(spotLine.getTitle())
            .area(spotLine.getArea())
            .spotsCount(spotLine.getSpots() != null ? spotLine.getSpots().size() : 0)
            .scheduledDate(ur.getScheduledDate())
            .status(ur.getStatus())
            .completedAt(ur.getCompletedAt() != null ? ur.getCompletedAt().toString() : null)
            .parentSpotLineId(spotLine.getParentSpotLine() != null ? spotLine.getParentSpotLine().getId().toString() : spotLine.getId().toString())
            .createdAt(ur.getCreatedAt() != null ? ur.getCreatedAt().toString() : null)
            .build();
    }
}
