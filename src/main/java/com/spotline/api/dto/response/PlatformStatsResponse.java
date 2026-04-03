package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

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
