package com.spotline.api.controller;

import com.spotline.api.dto.request.CreateRouteRequest;
import com.spotline.api.dto.request.UpdateRouteRequest;
import com.spotline.api.dto.response.RouteDetailResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final AuthUtil authUtil;

    @GetMapping("/{slug}")
    public ResponseEntity<RouteDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(routeService.getDetailBySlug(slug));
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<RoutePreviewResponse>> popular(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String theme,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(routeService.getPopularPreviews(area, theme, pageable));
    }

    @GetMapping("/slugs")
    public ResponseEntity<List<SlugResponse>> slugs() {
        return ResponseEntity.ok(routeService.getAllSlugs());
    }

    @PostMapping
    public ResponseEntity<RouteDetailResponse> create(@Valid @RequestBody CreateRouteRequest request) {
        String userId = authUtil.requireUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createAndReturn(request, userId, "user"));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<RouteDetailResponse> update(
            @PathVariable String slug,
            @Valid @RequestBody UpdateRouteRequest request) {
        return ResponseEntity.ok(routeService.update(slug, request, authUtil.requireUserId()));
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        routeService.delete(slug, authUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }
}
