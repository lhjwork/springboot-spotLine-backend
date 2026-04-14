package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "기간 비교 응답")
@Data
@Builder
public class PeriodComparisonResponse {
    private long currentSpots;
    private long currentSpotLines;
    private long currentViews;
    private long currentLikes;
    private long previousSpots;
    private long previousSpotLines;
    private long previousViews;
    private long previousLikes;
    private double spotsChangeRate;
    private double spotLinesChangeRate;
    private double viewsChangeRate;
    private double likesChangeRate;
}
