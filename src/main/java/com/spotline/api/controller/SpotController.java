package com.spotline.api.controller;

import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.request.UpdateSpotRequest;
import com.spotline.api.dto.response.DiscoverResponse;
import com.spotline.api.dto.response.SpotDetailResponse;
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

@RestController
@RequestMapping("/api/v2/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

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

    @GetMapping
    public ResponseEntity<Page<SpotDetailResponse>> list(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(spotService.list(area, category, pageable));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.create(request));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<SpotDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateSpotRequest request) {
        return ResponseEntity.ok(spotService.update(slug, request));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        spotService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<SpotDetailResponse>> bulkCreate(
            @Valid @RequestBody @Size(max = 50, message = "한 번에 최대 50개까지 등록 가능합니다") List<CreateSpotRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.bulkCreate(requests));
    }
}
