package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.RouteSpot;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreateRouteRequest;
import com.spotline.api.dto.response.RouteDetailResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final SpotRepository spotRepository;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    public Route getBySlug(String slug) {
        return routeRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Route", slug));
    }

    public RouteDetailResponse getDetailBySlug(String slug) {
        Route route = getBySlug(slug);
        return RouteDetailResponse.from(route);
    }

    public Page<RoutePreviewResponse> getPopularPreviews(String area, String theme, Pageable pageable) {
        return getPopular(area, theme, pageable).map(RoutePreviewResponse::from);
    }

    @Transactional
    public RouteDetailResponse createAndReturn(CreateRouteRequest request) {
        Route route = create(request);
        return RouteDetailResponse.from(route);
    }

    public Page<Route> getPopular(String area, String theme, Pageable pageable) {
        if (area != null && theme != null) {
            return routeRepository.findByAreaAndThemeAndIsActiveTrueOrderByLikesCountDesc(
                    area, com.spotline.api.domain.enums.RouteTheme.valueOf(theme.toUpperCase()), pageable);
        } else if (area != null) {
            return routeRepository.findByAreaAndIsActiveTrueOrderByLikesCountDesc(area, pageable);
        } else if (theme != null) {
            return routeRepository.findByThemeAndIsActiveTrueOrderByLikesCountDesc(
                    com.spotline.api.domain.enums.RouteTheme.valueOf(theme.toUpperCase()), pageable);
        }
        return routeRepository.findByIsActiveTrueOrderByLikesCountDesc(pageable);
    }

    @Transactional
    public Route create(CreateRouteRequest request) {
        String slug = generateUniqueSlug(request.getTitle());

        Route route = Route.builder()
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .area(request.getArea())
                .creatorType("crew")
                .creatorName(request.getCreatorName())
                .build();

        // 변형 원본 연결
        if (request.getParentRouteId() != null) {
            Route parent = routeRepository.findById(request.getParentRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route", request.getParentRouteId().toString()));
            route.setParentRoute(parent);
        }

        // RouteSpot 추가
        int totalDuration = 0;
        int totalDistance = 0;
        for (int i = 0; i < request.getSpots().size(); i++) {
            CreateRouteRequest.RouteSpotRequest spotReq = request.getSpots().get(i);
            Spot spot = spotRepository.findById(spotReq.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Spot", spotReq.getSpotId().toString()));

            RouteSpot routeSpot = RouteSpot.builder()
                    .route(route)
                    .spot(spot)
                    .spotOrder(spotReq.getOrder() != null ? spotReq.getOrder() : i + 1)
                    .suggestedTime(spotReq.getSuggestedTime())
                    .stayDuration(spotReq.getStayDuration())
                    .walkingTimeToNext(spotReq.getWalkingTimeToNext())
                    .distanceToNext(spotReq.getDistanceToNext())
                    .transitionNote(spotReq.getTransitionNote())
                    .build();

            route.getSpots().add(routeSpot);

            if (spotReq.getStayDuration() != null) totalDuration += spotReq.getStayDuration();
            if (spotReq.getWalkingTimeToNext() != null) totalDuration += spotReq.getWalkingTimeToNext();
            if (spotReq.getDistanceToNext() != null) totalDistance += spotReq.getDistanceToNext();
        }

        route.setTotalDuration(totalDuration);
        route.setTotalDistance(totalDistance);

        return routeRepository.save(route);
    }

    public List<SlugResponse> getAllSlugs() {
        return routeRepository.findAllActiveSlugs().stream()
                .map(r -> SlugResponse.builder()
                        .slug(r.getSlug())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();
    }

    private String generateUniqueSlug(String title) {
        String base = slugify.slugify(title);
        if (base.isEmpty()) {
            base = UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = base;
        int counter = 1;
        while (routeRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
