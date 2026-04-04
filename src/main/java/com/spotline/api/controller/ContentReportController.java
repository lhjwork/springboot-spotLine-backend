package com.spotline.api.controller;

import com.spotline.api.domain.enums.ReportStatus;
import com.spotline.api.dto.request.CreateReportRequest;
import com.spotline.api.dto.request.ResolveReportRequest;
import com.spotline.api.dto.response.ReportResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.ContentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Report", description = "콘텐츠 신고 + 관리자 처리")
@RestController
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService reportService;
    private final AuthUtil authUtil;

    @Operation(summary = "콘텐츠 신고")
    @PostMapping("/api/v2/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody CreateReportRequest request) {
        return reportService.create(authUtil.requireUserId(), request);
    }

    @Operation(summary = "신고 목록 (관리자)")
    @GetMapping("/api/v2/admin/reports")
    public Page<ReportResponse> list(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return reportService.list(status, pageable);
    }

    @Operation(summary = "미처리 신고 수")
    @GetMapping("/api/v2/admin/reports/pending-count")
    public Map<String, Long> getPendingCount() {
        return Map.of("count", reportService.getPendingCount());
    }

    @Operation(summary = "신고 처리")
    @PutMapping("/api/v2/admin/reports/{id}/resolve")
    public ReportResponse resolve(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request) {
        return reportService.resolve(id, authUtil.requireUserId(), request);
    }
}
