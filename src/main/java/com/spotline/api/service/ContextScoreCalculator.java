package com.spotline.api.service;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;
import org.springframework.stereotype.Service;

@Service
public class ContextScoreCalculator {

    public double calculate(Spot spot, WeatherCondition currentWeather, TimeOfDay currentTime) {
        double weatherScore = calculateWeatherFit(spot, currentWeather);
        double timeScore = calculateTimeOfDayFit(spot, currentTime);
        return weatherScore * 0.5 + timeScore * 0.5;
    }

    private double calculateWeatherFit(Spot spot, WeatherCondition current) {
        if (spot.getBestWeatherCondition() == null || spot.getBestWeatherCondition() == WeatherCondition.ANY) {
            return 0.7;
        }
        if (spot.getBestWeatherCondition() == current) return 1.0;
        if (Boolean.TRUE.equals(spot.getIsIndoor()) && current == WeatherCondition.RAINY) return 0.9;
        if (Boolean.FALSE.equals(spot.getIsIndoor()) && current == WeatherCondition.SUNNY) return 0.9;
        if (Boolean.FALSE.equals(spot.getIsIndoor()) && current == WeatherCondition.RAINY) return 0.2;
        return 0.5;
    }

    private double calculateTimeOfDayFit(Spot spot, TimeOfDay current) {
        if (spot.getBestTimeOfDay() == null || spot.getBestTimeOfDay() == TimeOfDay.ANY) {
            return 0.7;
        }
        if (spot.getBestTimeOfDay() == current) return 1.0;
        if (isAdjacent(spot.getBestTimeOfDay(), current)) return 0.7;
        return 0.3;
    }

    private boolean isAdjacent(TimeOfDay a, TimeOfDay b) {
        return switch (a) {
            case DAWN -> b == TimeOfDay.MORNING || b == TimeOfDay.NIGHT;
            case MORNING -> b == TimeOfDay.DAWN || b == TimeOfDay.AFTERNOON;
            case AFTERNOON -> b == TimeOfDay.MORNING || b == TimeOfDay.SUNSET;
            case SUNSET -> b == TimeOfDay.AFTERNOON || b == TimeOfDay.NIGHT;
            case NIGHT -> b == TimeOfDay.SUNSET || b == TimeOfDay.DAWN;
            case ANY -> true;
        };
    }
}
