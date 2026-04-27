package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.WeatherCondition;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_cache", indexes = {
        @Index(name = "idx_weather_region_expires", columnList = "regionCode, expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String regionCode;  // KMA grid format "nx_ny" (e.g., "60_127")

    private Double temperature;  // °C

    @Enumerated(EnumType.STRING)
    private WeatherCondition condition;

    private Integer humidity;    // %

    private Double windSpeed;    // m/s

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // fetchedAt + 1h
}
