package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "플랫폼 통계 응답")
@Data
@Builder
public class PlatformStatsResponse {
    private long totalSpots;
    private long totalRoutes;
    private long totalComments;
    private long totalReports;
    private long totalSpotViews;
    private long totalRouteViews;
}
