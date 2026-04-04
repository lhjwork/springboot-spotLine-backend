package com.spotline.api.controller;

import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.dto.request.CreateSpotLineRequest;
import com.spotline.api.dto.request.UpdateSpotLineRequest;
import com.spotline.api.dto.response.SpotLineDetailResponse;
import com.spotline.api.dto.response.SpotLinePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.SpotLineService;
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

@Tag(name = "SpotLine", description = "SpotLine CRUD + 탐색")
@RestController
@RequestMapping("/api/v2/spotlines")
@RequiredArgsConstructor
public class SpotLineController {

    private final SpotLineService spotLineService;
    private final AuthUtil authUtil;

    @Operation(summary = "SpotLine 상세 조회 (slug)")
    @GetMapping("/{slug}")
    public ResponseEntity<SpotLineDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(spotLineService.getDetailBySlug(slug));
    }

    @Operation(summary = "인기 SpotLine 목록 조회")
    @GetMapping("/popular")
    public ResponseEntity<Page<SpotLinePreviewResponse>> popular(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        FeedSort feedSort = parseFeedSort(sort);
        return ResponseEntity.ok(spotLineService.getPopularPreviews(area, theme, keyword, feedSort, pageable));
    }

    @Operation(summary = "전체 SpotLine slug 목록 (SSR/sitemap)")
    @GetMapping("/slugs")
    public ResponseEntity<List<SlugResponse>> slugs() {
        return ResponseEntity.ok(spotLineService.getAllSlugs());
    }

    @Operation(summary = "SpotLine 생성")
    @PostMapping
    public ResponseEntity<SpotLineDetailResponse> create(@Valid @RequestBody CreateSpotLineRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spotLineService.createAndReturn(request, userId, "user"));
    }

    @Operation(summary = "SpotLine 수정")
    @PutMapping("/{slug}")
    public ResponseEntity<SpotLineDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSpotLineRequest request) {
        return ResponseEntity.ok(spotLineService.update(slug, request, authUtil.requireUserId()));
    }

    @Operation(summary = "SpotLine 삭제")
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        spotLineService.delete(slug, authUtil.requireUserId());
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
