package com.spotline.api.controller;

import com.spotline.api.dto.response.DailyContentTrendResponse;
import com.spotline.api.dto.response.PlatformStatsResponse;
import com.spotline.api.dto.response.PopularContentResponse;
import com.spotline.api.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ---- Admin Analytics (hasRole("ADMIN") via SecurityConfig) ----

    @GetMapping("/api/v2/admin/analytics/stats")
    public ResponseEntity<PlatformStatsResponse> getStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    @GetMapping("/api/v2/admin/analytics/popular-spots")
    public ResponseEntity<List<PopularContentResponse>> getPopularSpots() {
        return ResponseEntity.ok(analyticsService.getPopularSpots());
    }

    @GetMapping("/api/v2/admin/analytics/popular-routes")
    public ResponseEntity<List<PopularContentResponse>> getPopularRoutes() {
        return ResponseEntity.ok(analyticsService.getPopularRoutes());
    }

    @GetMapping("/api/v2/admin/analytics/daily-trend")
    public ResponseEntity<List<DailyContentTrendResponse>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getDailyTrend(days));
    }

    // ---- Public View Count (permitAll via SecurityConfig) ----

    @PostMapping("/api/v2/spots/{id}/view")
    public ResponseEntity<Void> incrementSpotView(@PathVariable UUID id) {
        analyticsService.incrementSpotView(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v2/routes/{id}/view")
    public ResponseEntity<Void> incrementRouteView(@PathVariable UUID id) {
        analyticsService.incrementRouteView(id);
        return ResponseEntity.ok().build();
    }
}
