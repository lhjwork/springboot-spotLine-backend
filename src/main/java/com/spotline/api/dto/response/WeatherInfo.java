package com.spotline.api.dto.response;

import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;

public record WeatherInfo(
        double temperature,
        WeatherCondition condition,
        int humidity,
        double windSpeed,
        TimeOfDay currentTimeOfDay
) {}
