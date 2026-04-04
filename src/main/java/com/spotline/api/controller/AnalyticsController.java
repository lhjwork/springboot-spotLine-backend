package com.spotline.api.controller;

import com.spotline.api.dto.response.DailyContentTrendResponse;
import com.spotline.api.dto.response.PlatformStatsResponse;
import com.spotline.api.dto.response.PopularContentResponse;
import com.spotline.api.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Analytics", description = "조회수 + 관리자 통계")
@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "플랫폼 전체 통계")
    @GetMapping("/api/v2/admin/analytics/stats")
    public ResponseEntity<PlatformStatsResponse> getStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    @Operation(summary = "인기 스팟 순위")
    @GetMapping("/api/v2/admin/analytics/popular-spots")
    public ResponseEntity<List<PopularContentResponse>> getPopularSpots() {
        return ResponseEntity.ok(analyticsService.getPopularSpots());
    }

    @Operation(summary = "인기 루트 순위")
    @GetMapping("/api/v2/admin/analytics/popular-routes")
    public ResponseEntity<List<PopularContentResponse>> getPopularRoutes() {
        return ResponseEntity.ok(analyticsService.getPopularRoutes());
    }

    @Operation(summary = "일별 콘텐츠 추이")
    @GetMapping("/api/v2/admin/analytics/daily-trend")
    public ResponseEntity<List<DailyContentTrendResponse>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getDailyTrend(days));
    }

    // ---- Public View Count (permitAll via SecurityConfig) ----

    @Operation(summary = "스팟 조회수 증가")
    @PostMapping("/api/v2/spots/{id}/view")
    public ResponseEntity<Void> incrementSpotView(@PathVariable UUID id) {
        analyticsService.incrementSpotView(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "루트 조회수 증가")
    @PostMapping("/api/v2/routes/{id}/view")
    public ResponseEntity<Void> incrementRouteView(@PathVariable UUID id) {
        analyticsService.incrementRouteView(id);
        return ResponseEntity.ok().build();
    }
}
