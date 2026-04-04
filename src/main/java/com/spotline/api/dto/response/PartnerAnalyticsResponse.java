package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "파트너 분석 응답")
@Data
@Builder
public class PartnerAnalyticsResponse {
    private UUID partnerId;
    private String spotTitle;
    private String period;
    private long totalScans;
    private long uniqueVisitors;
    private double conversionRate;
    private LocalDateTime lastScanAt;
    private List<DailyScanTrend> dailyTrend;

    @Data
    @AllArgsConstructor
    public static class DailyScanTrend {
        private String date;   // "2026-04-01"
        private long scans;
    }
}
