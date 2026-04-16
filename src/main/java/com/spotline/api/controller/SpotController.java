package com.spotline.api.controller;

import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.domain.enums.SpotStatus;
import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.request.RejectSpotRequest;
import com.spotline.api.dto.request.UpdateSpotRequest;
import com.spotline.api.dto.response.DiscoverResponse;
import com.spotline.api.dto.response.SpotLinePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.SpotDetailResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Spot", description = "스팟 CRUD + 탐색")
@RestController
@RequestMapping("/api/v2/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;
    private final AuthUtil authUtil;

    @Operation(summary = "QR Discovery — 현재 스팟 기반 추천")
    @GetMapping("/discover")
    public ResponseEntity<DiscoverResponse> discover(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "1.0") double radius,
            @RequestParam(required = false) java.util.UUID excludeSpotId) {
        if (lat == null || lng == null) {
            // No location — return popular fallback
            return ResponseEntity.ok(spotService.discover(37.5665, 126.9780, radius, excludeSpotId)); // Seoul center
        }
        return ResponseEntity.ok(spotService.discover(lat, lng, radius, excludeSpotId));
    }

    @Operation(summary = "내 스팟 목록")
    @GetMapping("/my")
    public ResponseEntity<Page<SpotDetailResponse>> mySpots(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        String userId = authUtil.requireUserId();
        SpotStatus spotStatus = parseSpotStatus(status);
        return ResponseEntity.ok(spotService.getMySpots(userId, spotStatus, pageable));
    }

    @Operation(summary = "검토 대기 스팟 목록 (어드민)")
    @GetMapping("/pending")
    public ResponseEntity<Page<SpotDetailResponse>> pendingSpots(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(spotService.getPendingSpots(pageable));
    }

    @Operation(summary = "검토 대기 스팟 수")
    @GetMapping("/pending/count")
    public ResponseEntity<Long> pendingCount() {
        return ResponseEntity.ok(spotService.countPending());
    }

    @Operation(summary = "스팟 승인 (어드민)")
    @PutMapping("/{slug}/approve")
    public ResponseEntity<SpotDetailResponse> approve(@PathVariable String slug) {
        String adminId = authUtil.requireUserId();
        return ResponseEntity.ok(spotService.approve(slug, adminId));
    }

    @Operation(summary = "스팟 반려 (어드민)")
    @PutMapping("/{slug}/reject")
    public ResponseEntity<SpotDetailResponse> reject(
            @PathVariable String slug,
            @Valid @RequestBody RejectSpotRequest request) {
        String adminId = authUtil.requireUserId();
        return ResponseEntity.ok(spotService.reject(slug, request.getReason(), adminId));
    }

    @Operation(summary = "스팟 상세 조회 (slug)")
    @GetMapping("/{slug}")
    public ResponseEntity<SpotDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(spotService.getBySlug(slug));
    }

    @Operation(summary = "스팟이 포함된 SpotLine 목록")
    @GetMapping("/{spotId}/spotlines")
    public ResponseEntity<List<SpotLinePreviewResponse>> getRoutesBySpotId(
            @PathVariable UUID spotId) {
        return ResponseEntity.ok(spotService.findSpotLinesBySpotId(spotId));
    }

    @Operation(summary = "스팟 목록 조회")
    @GetMapping
    public ResponseEntity<Page<SpotDetailResponse>> list(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        FeedSort feedSort = parseFeedSort(sort);
        return ResponseEntity.ok(spotService.list(area, category, keyword, feedSort, pageable));
    }

    @Operation(summary = "근처 스팟 조회")
    @GetMapping("/nearby")
    public ResponseEntity<List<SpotDetailResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1.0") double radius) {
        return ResponseEntity.ok(spotService.findNearby(lat, lng, radius));
    }

    @Operation(summary = "스팟 생성")
    @PostMapping
    public ResponseEntity<SpotDetailResponse> create(@Valid @RequestBody CreateSpotRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spotService.create(request, userId, "user"));
    }

    @Operation(summary = "스팟 수정")
    @PutMapping("/{slug}")
    public ResponseEntity<SpotDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(spotService.update(slug, request, authUtil.requireUserId()));
    }

    @Operation(summary = "스팟 삭제")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        spotService.delete(slug, authUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 스팟 slug 목록 (SSR/sitemap)")
    @GetMapping("/slugs")
    public ResponseEntity<List<SlugResponse>> slugs() {
        return ResponseEntity.ok(spotService.getAllSlugs());
    }

    @Operation(summary = "스팟 대량 생성 (최대 50개)")
    @PostMapping("/bulk")
    public ResponseEntity<List<SpotDetailResponse>> bulkCreate(
            @Valid @RequestBody @Size(max = 50, message = "한 번에 최대 50개까지 등록 가능합니다") List<CreateSpotRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.bulkCreate(requests));
    }

    private SpotStatus parseSpotStatus(String status) {
        if (status == null) return null;
        try {
            return SpotStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private FeedSort parseFeedSort(String sort) {
        if (sort == null) return FeedSort.POPULAR;
        try {
            return FeedSort.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FeedSort.POPULAR;
        }
    }
}
