package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {

    Optional<WeatherCache> findTopByRegionCodeAndExpiresAtAfterOrderByFetchedAtDesc(
            String regionCode, LocalDateTime now);
}
