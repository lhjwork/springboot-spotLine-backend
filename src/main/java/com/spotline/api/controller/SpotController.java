package com.spotline.api.controller;

import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.request.UpdateSpotRequest;
import com.spotline.api.dto.response.DiscoverResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.SpotDetailResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.SpotService;
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

@RestController
@RequestMapping("/api/v2/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;
    private final AuthUtil authUtil;

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

    @GetMapping("/{slug}")
    public ResponseEntity<SpotDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(spotService.getBySlug(slug));
    }

    @GetMapping("/{spotId}/routes")
    public ResponseEntity<List<RoutePreviewResponse>> getRoutesBySpotId(
            @PathVariable UUID spotId) {
        return ResponseEntity.ok(spotService.findRoutesBySpotId(spotId));
    }

    @GetMapping
    public ResponseEntity<Page<SpotDetailResponse>> list(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        FeedSort feedSort = parseFeedSort(sort);
        return ResponseEntity.ok(spotService.list(area, category, feedSort, pageable));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<SpotDetailResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1.0") double radius) {
        return ResponseEntity.ok(spotService.findNearby(lat, lng, radius));
    }

    @PostMapping
    public ResponseEntity<SpotDetailResponse> create(@Valid @RequestBody CreateSpotRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spotService.create(request, userId, "user"));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<SpotDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(spotService.update(slug, request, authUtil.requireUserId()));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        spotService.delete(slug, authUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slugs")
    public ResponseEntity<List<SlugResponse>> slugs() {
        return ResponseEntity.ok(spotService.getAllSlugs());
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<SpotDetailResponse>> bulkCreate(
            @Valid @RequestBody @Size(max = 50, message = "한 번에 최대 50개까지 등록 가능합니다") List<CreateSpotRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.bulkCreate(requests));
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
