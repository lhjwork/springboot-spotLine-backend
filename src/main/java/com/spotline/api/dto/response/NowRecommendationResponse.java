package com.spotline.api.dto.response;

import com.spotline.api.domain.enums.TimeOfDay;

import java.util.List;

public record NowRecommendationResponse(
        WeatherInfo weather,
        List<RecommendedSpotResponse> spots,
        TimeOfDay timeContext
) {}
