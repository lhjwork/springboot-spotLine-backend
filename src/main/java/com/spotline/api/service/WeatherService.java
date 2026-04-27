package com.spotline.api.service;

import com.spotline.api.domain.entity.WeatherCache;
import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;
import com.spotline.api.domain.repository.WeatherCacheRepository;
import com.spotline.api.dto.response.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherCacheRepository cacheRepo;
    private final WebClient.Builder webClientBuilder;

    @Value("${weather.api.key:}")
    private String apiKey;

    public WeatherInfo getCurrentWeather(double lat, double lng) {
        String regionCode = convertToGrid(lat, lng);

        // 1. DB 캐시 조회
        Optional<WeatherCache> cached = cacheRepo
                .findTopByRegionCodeAndExpiresAtAfterOrderByFetchedAtDesc(regionCode, LocalDateTime.now());
        if (cached.isPresent()) {
            return toWeatherInfo(cached.get());
        }

        // 2. KMA API 호출
        try {
            WeatherCache fresh = fetchFromKma(regionCode);
            cacheRepo.save(fresh);
            return toWeatherInfo(fresh);
        } catch (Exception e) {
            log.warn("KMA API 호출 실패, 마지막 유효 캐시 반환 시도: {}", e.getMessage());
            // graceful degradation: 만료된 캐시라도 반환
            Optional<WeatherCache> lastValid = cacheRepo
                    .findTopByRegionCodeAndExpiresAtAfterOrderByFetchedAtDesc(regionCode, LocalDateTime.MIN);
            if (lastValid.isPresent()) {
                return toWeatherInfo(lastValid.get());
            }
            // 캐시도 없으면 기본값 반환
            return new WeatherInfo(20.0, WeatherCondition.SUNNY, 50, 2.0, getCurrentTimeOfDay());
        }
    }

    public TimeOfDay getCurrentTimeOfDay() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 7) return TimeOfDay.DAWN;
        if (hour >= 7 && hour < 11) return TimeOfDay.MORNING;
        if (hour >= 11 && hour < 16) return TimeOfDay.AFTERNOON;
        if (hour >= 16 && hour < 19) return TimeOfDay.SUNSET;
        return TimeOfDay.NIGHT;
    }

    /**
     * 위경도 → KMA 기상청 격자 좌표 변환 (LCC Projection)
     */
    private String convertToGrid(double lat, double lng) {
        double RE = 6371.00877;
        double GRID = 5.0;
        double SLAT1 = 30.0;
        double SLAT2 = 60.0;
        double OLON = 126.0;
        double OLAT = 38.0;
        double XO = 43;
        double YO = 136;

        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lng * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return nx + "_" + ny;
    }

    private WeatherCache fetchFromKma(String regionCode) {
        if (apiKey.isEmpty()) {
            log.warn("KMA API 키가 설정되지 않았습니다, 기본 날씨 반환");
            return buildDefaultCache(regionCode);
        }

        String[] parts = regionCode.split("_");
        int nx = Integer.parseInt(parts[0]);
        int ny = Integer.parseInt(parts[1]);

        LocalDateTime now = LocalDateTime.now();
        // KMA 단기예보: 매시 30분 이후 발표 → 직전 정시 base_time 사용
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = now.getHour();
        if (now.getMinute() < 45) hour = hour - 1;
        if (hour < 0) {
            hour = 23;
            baseDate = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        String baseTime = String.format("%02d30", hour);

        try {
            Map<?, ?> result = webClientBuilder.build()
                    .get()
                    .uri("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst"
                                    + "?serviceKey={key}&numOfRows=60&pageNo=1&dataType=JSON"
                                    + "&base_date={date}&base_time={time}&nx={nx}&ny={ny}",
                            apiKey, baseDate, baseTime, nx, ny)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseKmaResponse(result, regionCode);
        } catch (Exception e) {
            log.error("KMA API 호출 오류: {}", e.getMessage());
            return buildDefaultCache(regionCode);
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherCache parseKmaResponse(Map<?, ?> result, String regionCode) {
        double temperature = 20.0;
        WeatherCondition condition = WeatherCondition.SUNNY;
        int humidity = 50;
        double windSpeed = 2.0;

        try {
            Map<?, ?> response = (Map<?, ?>) result.get("response");
            Map<?, ?> body = (Map<?, ?>) response.get("body");
            Map<?, ?> items = (Map<?, ?>) body.get("items");
            var itemList = (java.util.List<Map<String, Object>>) items.get("item");

            for (Map<String, Object> item : itemList) {
                String category = (String) item.get("category");
                String value = String.valueOf(item.get("fcstValue"));

                switch (category) {
                    case "T1H" -> temperature = Double.parseDouble(value);
                    case "REH" -> humidity = Integer.parseInt(value);
                    case "WSD" -> windSpeed = Double.parseDouble(value);
                    case "PTY" -> {
                        int pty = Integer.parseInt(value);
                        condition = switch (pty) {
                            case 1, 5 -> WeatherCondition.RAINY;
                            case 2, 6 -> WeatherCondition.RAINY;
                            case 3, 7 -> WeatherCondition.SNOWY;
                            default -> WeatherCondition.SUNNY;
                        };
                    }
                    case "SKY" -> {
                        if (condition == WeatherCondition.SUNNY) {
                            int sky = Integer.parseInt(value);
                            condition = sky >= 6 ? WeatherCondition.CLOUDY : WeatherCondition.SUNNY;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("KMA 응답 파싱 실패, 기본값 사용: {}", e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        return WeatherCache.builder()
                .regionCode(regionCode)
                .temperature(temperature)
                .condition(condition)
                .humidity(humidity)
                .windSpeed(windSpeed)
                .fetchedAt(now)
                .expiresAt(now.plusHours(1))
                .build();
    }

    private WeatherCache buildDefaultCache(String regionCode) {
        LocalDateTime now = LocalDateTime.now();
        return WeatherCache.builder()
                .regionCode(regionCode)
                .temperature(20.0)
                .condition(WeatherCondition.SUNNY)
                .humidity(50)
                .windSpeed(2.0)
                .fetchedAt(now)
                .expiresAt(now.plusHours(1))
                .build();
    }

    private WeatherInfo toWeatherInfo(WeatherCache cache) {
        return new WeatherInfo(
                cache.getTemperature(),
                cache.getCondition(),
                cache.getHumidity(),
                cache.getWindSpeed(),
                getCurrentTimeOfDay()
        );
    }
}
