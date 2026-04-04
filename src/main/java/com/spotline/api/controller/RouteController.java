package com.spotline.api.controller;

import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.dto.request.CreateRouteRequest;
import com.spotline.api.dto.request.UpdateRouteRequest;
import com.spotline.api.dto.response.RouteDetailResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Route", description = "루트 CRUD + 탐색")
@RestController
@RequestMapping("/api/v2/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final AuthUtil authUtil;

    @Operation(summary = "루트 상세 조회 (slug)")
    @GetMapping("/{slug}")
    public ResponseEntity<RouteDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(routeService.getDetailBySlug(slug));
    }

    @Operation(summary = "인기 루트 목록 조회")
    @GetMapping("/popular")
    public ResponseEntity<Page<RoutePreviewResponse>> popular(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        FeedSort feedSort = parseFeedSort(sort);
        return ResponseEntity.ok(routeService.getPopularPreviews(area, theme, keyword, feedSort, pageable));
    }

    @Operation(summary = "전체 루트 slug 목록 (SSR/sitemap)")
    @GetMapping("/slugs")
    public ResponseEntity<List<SlugResponse>> slugs() {
        return ResponseEntity.ok(routeService.getAllSlugs());
    }

    @Operation(summary = "루트 생성")
    @PostMapping
    public ResponseEntity<RouteDetailResponse> create(@Valid @RequestBody CreateRouteRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createAndReturn(request, userId, "user"));
    }

    @Operation(summary = "루트 수정")
    @PutMapping("/{slug}")
    public ResponseEntity<RouteDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateRouteRequest request) {
        return ResponseEntity.ok(routeService.update(slug, request, authUtil.requireUserId()));
    }

    @Operation(summary = "루트 삭제")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        routeService.delete(slug, authUtil.requireUserId());
        return ResponseEntity.noContent().build();
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
