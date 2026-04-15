package com.spotline.api.controller;

import com.spotline.api.dto.response.CheckinPatternResponse;
import com.spotline.api.dto.response.CheckinStatsResponse;
import com.spotline.api.dto.response.TopSpotCheckinResponse;
import com.spotline.api.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Admin Checkin", description = "체크인 통계 (어드민)")
@RestController
@RequestMapping("/api/v2/admin/checkins")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "체크인 통계")
    @GetMapping("/stats")
    public CheckinStatsResponse getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return checkinService.getCheckinStats(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    @Operation(summary = "인기 스팟 (체크인 기준)")
    @GetMapping("/top-spots")
    public TopSpotCheckinResponse getTopSpots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return checkinService.getTopSpots(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), limit);
    }

    @Operation(summary = "체크인 패턴 (시간대/요일)")
    @GetMapping("/pattern")
    public CheckinPatternResponse getPattern(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate now = LocalDate.now();
        if (from == null) from = now.minusDays(30);
        if (to == null) to = now;
        return checkinService.getCheckinPattern(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
}
