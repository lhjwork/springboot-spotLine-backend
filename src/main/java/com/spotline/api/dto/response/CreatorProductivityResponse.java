package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "크리에이터 생산성 응답")
@Data
@Builder
public class CreatorProductivityResponse {
    private String creatorId;
    private String creatorName;
    private String creatorType;
    private long spotCount;
    private long spotLineCount;
    private long totalViews;
    private long totalLikes;
    private double avgViewsPerContent;
}
