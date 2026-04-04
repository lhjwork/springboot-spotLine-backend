package com.spotline.api.controller;

import com.spotline.api.infrastructure.place.PlaceApiService;
import com.spotline.api.infrastructure.place.PlaceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Place", description = "네이버/카카오 Place API 프록시 (24h 캐싱)")
@RestController
@RequestMapping("/api/v2/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceApiService placeApiService;

    @Operation(summary = "장소 검색 (네이버/카카오)")
    @GetMapping("/search")
    public ResponseEntity<List<PlaceInfo>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "kakao") String provider,
            @RequestParam(defaultValue = "15") int size) {
        List<PlaceInfo> results = switch (provider) {
            case "naver" -> placeApiService.searchNaver(query, size);
            case "kakao" -> placeApiService.searchKakao(query, size);
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "장소 상세 조회")
    @GetMapping("/{provider}/{placeId}")
    public ResponseEntity<PlaceInfo> detail(
            @PathVariable String provider,
            @PathVariable String placeId) {
        PlaceInfo info = placeApiService.getPlaceDetail(provider, placeId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }
}
