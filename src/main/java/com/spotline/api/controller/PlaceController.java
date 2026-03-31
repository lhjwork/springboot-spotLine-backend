package com.spotline.api.controller;

import com.spotline.api.infrastructure.place.PlaceApiService;
import com.spotline.api.infrastructure.place.PlaceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceApiService placeApiService;

    /**
     * Place API 검색 (크루 큐레이션 도구용)
     */
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

    /**
     * Place 상세 조회 (캐싱 적용, 24h TTL)
     */
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
