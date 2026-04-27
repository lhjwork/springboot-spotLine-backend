package com.spotline.api.controller;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.response.NowRecommendationResponse;
import com.spotline.api.dto.response.RecommendedSpotResponse;
import com.spotline.api.dto.response.WeatherInfo;
import com.spotline.api.service.ContextScoreCalculator;
import com.spotline.api.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Tag(name = "Recommendations", description = "추천 API")
@RestController
@RequestMapping("/api/v2/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final WeatherService weatherService;
    private final ContextScoreCalculator contextScoreCalculator;
    private final SpotRepository spotRepository;

    @Value("${aws.s3.bucket:}")
    private String s3Bucket;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String s3Region;

    @Operation(summary = "지금 추천", description = "현재 날씨/시간 기반 맥락 추천 Spot 목록")
    @GetMapping("/now")
    public ResponseEntity<NowRecommendationResponse> getContextualRecommendations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") int size
    ) {
        WeatherInfo weather = weatherService.getCurrentWeather(lat, lng);
        TimeOfDay timeOfDay = weatherService.getCurrentTimeOfDay();
        WeatherCondition currentWeather = weather.condition();

        // 상위 50개 인기 Spot 후보
        List<Spot> candidates = spotRepository
                .findApprovedOrderByViewsCountDesc(PageRequest.of(0, 50))
                .getContent();

        String s3BaseUrl = getS3BaseUrl();

        // contextScore로 재정렬 → 상위 size개
        List<RecommendedSpotResponse> rankedSpots = candidates.stream()
                .map(spot -> {
                    double score = contextScoreCalculator.calculate(spot, currentWeather, timeOfDay);
                    return RecommendedSpotResponse.from(spot, score, s3BaseUrl);
                })
                .sorted(Comparator.comparingDouble(RecommendedSpotResponse::getContextScore).reversed())
                .limit(size)
                .toList();

        return ResponseEntity.ok(new NowRecommendationResponse(weather, rankedSpots, timeOfDay));
    }

    private String getS3BaseUrl() {
        if (s3Bucket.isEmpty()) return null;
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com";
    }
}
