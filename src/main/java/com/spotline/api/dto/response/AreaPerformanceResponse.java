package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "지역별 성과 응답")
@Data
@Builder
public class AreaPerformanceResponse {
    private String area;
    private long spotCount;
    private long spotLineCount;
    private long totalViews;
    private long totalLikes;
    private double avgViewsPerSpot;
}
