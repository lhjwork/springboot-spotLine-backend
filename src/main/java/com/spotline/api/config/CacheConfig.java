package com.spotline.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${place.cache.ttl-hours:24}")
    private int ttlHours;

    @Value("${place.cache.max-size:2000}")
    private int maxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager placeCacheManager = new CaffeineCacheManager("placeInfo");
        placeCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlHours, TimeUnit.HOURS)
                .recordStats());

        CaffeineCacheManager analyticsCacheManager = new CaffeineCacheManager(
                "analyticsContentPerf", "analyticsCreatorProd", "analyticsAreaPerf", "analyticsPeriodComp");
        analyticsCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());

        CompositeCacheManager composite = new CompositeCacheManager();
        composite.setCacheManagers(java.util.List.of(placeCacheManager, analyticsCacheManager));
        return composite;
    }
}
