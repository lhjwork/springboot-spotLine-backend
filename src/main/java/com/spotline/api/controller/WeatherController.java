package com.spotline.api.controller;

import com.spotline.api.dto.response.WeatherInfo;
import com.spotline.api.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Weather", description = "날씨 정보 API")
@RestController
@RequestMapping("/api/v2/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "현재 날씨 조회", description = "위경도 기반 현재 날씨 정보 반환 (KMA 단기예보, 1시간 캐싱)")
    @GetMapping("/current")
    public ResponseEntity<WeatherInfo> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return ResponseEntity.ok(weatherService.getCurrentWeather(lat, lng));
    }
}
