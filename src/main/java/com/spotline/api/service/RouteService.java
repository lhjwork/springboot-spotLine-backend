package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.RouteSpot;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.domain.enums.RouteTheme;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.infrastructure.s3.S3Service;
import com.spotline.api.dto.request.CreateRouteRequest;
import com.spotline.api.dto.request.UpdateRouteRequest;
import com.spotline.api.dto.response.RouteDetailResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final S3Service s3Service;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    public Route getBySlug(String slug) {
        return routeRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Route", slug));
    }

    public RouteDetailResponse getDetailBySlug(String slug) {
        Route route = getBySlug(slug);
        return RouteDetailResponse.from(route);
    }

    public Page<RoutePreviewResponse> getPopularPreviews(
            String area, String theme, String keyword, FeedSort sort, Pageable pageable) {
        FeedSort effectiveSort = (sort != null) ? sort : FeedSort.POPULAR;
        Page<Route> routes;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (effectiveSort == FeedSort.NEWEST) {
            routes = hasKeyword
                ? getNewestWithKeyword(area, theme, keyword, pageable)
                : getNewest(area, theme, pageable);
        } else {
            routes = hasKeyword
                ? getPopularWithKeyword(area, theme, keyword, pageable)
                : getPopular(area, theme, pageable);
        }

        String s3BaseUrl = getS3BaseUrl();
        return routes.map(route -> RoutePreviewResponse.from(route, s3BaseUrl));
    }

    private Page<Route> getNewest(String area, String theme, Pageable pageable) {
        if (area != null && theme != null) {
            return routeRepository.findByAreaLikeAndThemeAndNewest(
                    area, RouteTheme.valueOf(theme.toUpperCase()), pageable);
        } else if (area != null) {
            return routeRepository.findByAreaLikeAndNewest(area, pageable);
        } else if (theme != null) {
            return routeRepository.findByThemeAndIsActiveTrueOrderByCreatedAtDesc(
                    RouteTheme.valueOf(theme.toUpperCase()), pageable);
        }
        return routeRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    private Page<Route> getPopularWithKeyword(String area, String theme, String keyword, Pageable pageable) {
        if (area != null && theme != null) {
            return routeRepository.findByAreaLikeAndThemeAndKeywordAndPopular(
                    area, RouteTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return routeRepository.findByAreaLikeAndKeywordAndPopular(area, keyword, pageable);
        } else if (theme != null) {
            return routeRepository.findByThemeAndKeywordAndPopular(
                    RouteTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        }
        return routeRepository.findByKeywordAndPopular(keyword, pageable);
    }

    private Page<Route> getNewestWithKeyword(String area, String theme, String keyword, Pageable pageable) {
        if (area != null && theme != null) {
            return routeRepository.findByAreaLikeAndThemeAndKeywordAndNewest(
                    area, RouteTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return routeRepository.findByAreaLikeAndKeywordAndNewest(area, keyword, pageable);
        } else if (theme != null) {
            return routeRepository.findByThemeAndKeywordAndNewest(
                    RouteTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        }
        return routeRepository.findByKeywordAndNewest(keyword, pageable);
    }

    @Transactional
    public RouteDetailResponse createAndReturn(CreateRouteRequest request, String userId, String creatorType) {
        Route route = create(request, userId, creatorType);
        return RouteDetailResponse.from(route);
    }

    public Page<Route> getPopular(String area, String theme, Pageable pageable) {
        if (area != null && theme != null) {
            return routeRepository.findByAreaLikeAndThemeAndPopular(
                    area, com.spotline.api.domain.enums.RouteTheme.valueOf(theme.toUpperCase()), pageable);
        } else if (area != null) {
            return routeRepository.findByAreaLikeAndPopular(area, pageable);
        } else if (theme != null) {
            return routeRepository.findByThemeAndIsActiveTrueOrderByLikesCountDesc(
                    com.spotline.api.domain.enums.RouteTheme.valueOf(theme.toUpperCase()), pageable);
        }
        return routeRepository.findByIsActiveTrueOrderByLikesCountDesc(pageable);
    }

    @Transactional
    public Route create(CreateRouteRequest request, String userId, String creatorType) {
        String slug = generateUniqueSlug(request.getTitle());

        Route route = Route.builder()
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .theme(request.getTheme())
                .area(request.getArea())
                .creatorType(creatorType)
                .creatorId(userId)
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

    @Transactional
    public RouteDetailResponse update(String slug, UpdateRouteRequest request, String userId) {
        Route route = getBySlug(slug);
        verifyOwnership(route.getCreatorId(), userId);

        if (request.getTitle() != null) route.setTitle(request.getTitle());
        if (request.getDescription() != null) route.setDescription(request.getDescription());
        if (request.getTheme() != null) route.setTheme(request.getTheme());
        if (request.getArea() != null) route.setArea(request.getArea());

        if (request.getSpots() != null) {
            route.getSpots().clear(); // orphanRemoval 자동 삭제

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
        }

        return RouteDetailResponse.from(routeRepository.save(route));
    }

    @Transactional
    public void delete(String slug, String userId) {
        Route route = getBySlug(slug);
        verifyOwnership(route.getCreatorId(), userId);
        route.setIsActive(false);
        routeRepository.save(route);
    }

    private void verifyOwnership(String creatorId, String userId) {
        if (creatorId != null && !creatorId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 콘텐츠만 수정/삭제할 수 있습니다");
        }
    }

    public List<SlugResponse> getAllSlugs() {
        return routeRepository.findAllActiveSlugs().stream()
                .map(r -> SlugResponse.builder()
                        .slug(r.getSlug())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();
    }

    private String getS3BaseUrl() {
        return s3Service.getPublicUrl("").replaceAll("/$", "");
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
