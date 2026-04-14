package com.spotline.api.controller;

import com.spotline.api.dto.response.*;
import com.spotline.api.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @Operation(summary = "인기 SpotLine 순위")
    @GetMapping("/api/v2/admin/analytics/popular-spotlines")
    public ResponseEntity<List<PopularContentResponse>> getPopularSpotLines() {
        return ResponseEntity.ok(analyticsService.getPopularSpotLines());
    }

    @Operation(summary = "일별 콘텐츠 추이")
    @GetMapping("/api/v2/admin/analytics/daily-trend")
    public ResponseEntity<List<DailyContentTrendResponse>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getDailyTrend(days));
    }

    // ---- BI Analytics ----

    @Operation(summary = "콘텐츠 퍼포먼스")
    @GetMapping("/api/v2/admin/analytics/content-performance")
    public ResponseEntity<List<ContentPerformanceResponse>> getContentPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "spot") String type,
            @RequestParam(defaultValue = "views") String sort,
            @RequestParam(defaultValue = "50") int limit) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return ResponseEntity.ok(analyticsService.getContentPerformance(from, to, type, sort, limit));
    }

    @Operation(summary = "크리에이터 생산성")
    @GetMapping("/api/v2/admin/analytics/creator-productivity")
    public ResponseEntity<List<CreatorProductivityResponse>> getCreatorProductivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return ResponseEntity.ok(analyticsService.getCreatorProductivity(from, to));
    }

    @Operation(summary = "지역별 성과")
    @GetMapping("/api/v2/admin/analytics/area-performance")
    public ResponseEntity<List<AreaPerformanceResponse>> getAreaPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return ResponseEntity.ok(analyticsService.getAreaPerformance(from, to));
    }

    @Operation(summary = "기간 비교")
    @GetMapping("/api/v2/admin/analytics/period-comparison")
    public ResponseEntity<PeriodComparisonResponse> getPeriodComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return ResponseEntity.ok(analyticsService.getPeriodComparison(from, to));
    }

    // ---- Public View Count (permitAll via SecurityConfig) ----

    @Operation(summary = "스팟 조회수 증가")
    @PostMapping("/api/v2/spots/{id}/view")
    public ResponseEntity<Void> incrementSpotView(@PathVariable UUID id) {
        analyticsService.incrementSpotView(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "SpotLine 조회수 증가")
    @PostMapping("/api/v2/spotlines/{id}/view")
    public ResponseEntity<Void> incrementSpotLineView(@PathVariable UUID id) {
        analyticsService.incrementSpotLineView(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "블로그 조회수 증가")
    @PostMapping("/api/v2/blogs/{id}/view")
    public ResponseEntity<Void> incrementBlogView(@PathVariable UUID id) {
        analyticsService.incrementBlogView(id);
        return ResponseEntity.ok().build();
    }
}
