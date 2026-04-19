package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.NotificationType;
import com.spotline.api.domain.enums.SpotStatus;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.request.MediaItemRequest;
import com.spotline.api.dto.request.UpdateSpotRequest;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.dto.response.DiscoverResponse;
import com.spotline.api.dto.response.SpotLinePreviewResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.SpotDetailResponse;
import com.spotline.api.dto.response.SpotPartnerInfo;
import com.spotline.api.infrastructure.place.PlaceApiService;
import com.spotline.api.infrastructure.place.PlaceInfo;
import com.spotline.api.infrastructure.s3.S3Service;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpotService {

    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final PlaceApiService placeApiService;
    private final S3Service s3Service;
    private final PartnerService partnerService;
    private final NotificationService notificationService;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    /**
     * Spot 상세 조회 — DB 데이터 + Place API 병합
     */
    public SpotDetailResponse getBySlug(String slug) {
        Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));

        PlaceInfo placeInfo = resolvePlaceInfo(spot);
        SpotPartnerInfo partnerInfo = partnerService.getPartnerInfoBySpotId(spot.getId());
        return SpotDetailResponse.from(spot, placeInfo, getS3BaseUrl(), partnerInfo);
    }

    /**
     * 특정 Spot이 포함된 활성 SpotLine 프리뷰 목록 반환 (최대 10개)
     */
    public List<SpotLinePreviewResponse> findSpotLinesBySpotId(UUID spotId) {
        spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", spotId.toString()));

        List<SpotLine> spotLines = spotLineRepository.findActiveSpotLinesBySpotId(spotId);

        String s3BaseUrl = getS3BaseUrl();
        return spotLines.stream()
                .limit(10)
                .map(sl -> SpotLinePreviewResponse.from(sl, s3BaseUrl))
                .toList();
    }

    /**
     * Spot 목록 조회 (정렬 지원)
     */
    public Page<SpotDetailResponse> list(String area, String category, String keyword, FeedSort sort, Pageable pageable) {
        Page<Spot> spots;
        FeedSort effectiveSort = (sort != null) ? sort : FeedSort.POPULAR;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (effectiveSort == FeedSort.NEWEST) {
            spots = hasKeyword
                ? listByNewestWithKeyword(area, category, keyword, pageable)
                : listByNewest(area, category, pageable);
        } else {
            spots = hasKeyword
                ? listByPopularWithKeyword(area, category, keyword, pageable)
                : listByPopular(area, category, pageable);
        }

        String s3BaseUrl = getS3BaseUrl();
        return spots.map(spot -> SpotDetailResponse.from(spot, null, s3BaseUrl));
    }

    private Page<Spot> listByPopular(String area, String category, Pageable pageable) {
        if (area != null && category != null) {
            return spotRepository.findByAreaLikeAndCategoryAndPopular(
                    area, SpotCategory.valueOf(category.toUpperCase()), pageable);
        } else if (area != null) {
            return spotRepository.findByAreaLikeAndPopular(area, pageable);
        } else if (category != null) {
            return spotRepository.findApprovedByCategoryOrderByViewsCountDesc(
                    SpotCategory.valueOf(category.toUpperCase()), pageable);
        }
        return spotRepository.findApprovedOrderByViewsCountDesc(pageable);
    }

    private Page<Spot> listByNewest(String area, String category, Pageable pageable) {
        if (area != null && category != null) {
            return spotRepository.findByAreaLikeAndCategoryAndNewest(
                    area, SpotCategory.valueOf(category.toUpperCase()), pageable);
        } else if (area != null) {
            return spotRepository.findByAreaLikeAndNewest(area, pageable);
        } else if (category != null) {
            return spotRepository.findApprovedByCategoryOrderByCreatedAtDesc(
                    SpotCategory.valueOf(category.toUpperCase()), pageable);
        }
        return spotRepository.findApprovedOrderByCreatedAtDesc(pageable);
    }

    private Page<Spot> listByPopularWithKeyword(String area, String category, String keyword, Pageable pageable) {
        if (area != null && category != null) {
            return spotRepository.findByAreaLikeAndCategoryAndKeywordAndPopular(
                    area, SpotCategory.valueOf(category.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return spotRepository.findByAreaLikeAndKeywordAndPopular(area, keyword, pageable);
        } else if (category != null) {
            return spotRepository.findByCategoryAndKeywordAndPopular(
                    SpotCategory.valueOf(category.toUpperCase()), keyword, pageable);
        }
        return spotRepository.findByKeywordAndPopular(keyword, pageable);
    }

    private Page<Spot> listByNewestWithKeyword(String area, String category, String keyword, Pageable pageable) {
        if (area != null && category != null) {
            return spotRepository.findByAreaLikeAndCategoryAndKeywordAndNewest(
                    area, SpotCategory.valueOf(category.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return spotRepository.findByAreaLikeAndKeywordAndNewest(area, keyword, pageable);
        } else if (category != null) {
            return spotRepository.findByCategoryAndKeywordAndNewest(
                    SpotCategory.valueOf(category.toUpperCase()), keyword, pageable);
        }
        return spotRepository.findByKeywordAndNewest(keyword, pageable);
    }

    /**
     * 근처 Spot 검색
     */
    public List<SpotDetailResponse> findNearby(double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Spot> spots = spotRepository.findNearby(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta);

        String s3Base = getS3BaseUrl();
        return spots.stream()
                .map(spot -> SpotDetailResponse.from(spot, null, s3Base))
                .toList();
    }

    /**
     * Spot 생성
     */
    @Transactional
    public SpotDetailResponse create(CreateSpotRequest request, String userId, String creatorType) {
        String slug = generateUniqueSlug(request.getTitle());

        Spot spot = Spot.builder()
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .source(request.getSource())
                .crewNote(request.getCrewNote())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .area(request.getArea())
                .sido(request.getSido())
                .sigungu(request.getSigungu())
                .dong(request.getDong())
                .blogUrl(request.getBlogUrl())
                .instagramUrl(request.getInstagramUrl())
                .websiteUrl(request.getWebsiteUrl())
                .naverPlaceId(request.getNaverPlaceId())
                .kakaoPlaceId(request.getKakaoPlaceId())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .media(request.getMedia() != null ? request.getMedia() : new ArrayList<>())
                .creatorType(creatorType)
                .creatorId(userId)
                .creatorName(request.getCreatorName())
                .status("user".equals(creatorType) ? SpotStatus.PENDING : SpotStatus.APPROVED)
                .build();

        spot = spotRepository.save(spot);

        // mediaItems 처리
        if (request.getMediaItems() != null && !request.getMediaItems().isEmpty()) {
            addMediaItems(spot, request.getMediaItems());
            spot = spotRepository.save(spot);
        }

        return SpotDetailResponse.from(spot, null, getS3BaseUrl());
    }

    /**
     * Spot 수정
     */
    @Transactional
    public SpotDetailResponse update(String slug, UpdateSpotRequest request, String userId) {
        Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));
        verifyOwnership(spot.getCreatorId(), userId);

        // User can only edit PENDING or REJECTED spots
        if ("user".equals(spot.getCreatorType()) && spot.getStatus() == SpotStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인된 Spot은 수정할 수 없습니다");
        }

        if (request.getTitle() != null) spot.setTitle(request.getTitle());
        if (request.getDescription() != null) spot.setDescription(request.getDescription());
        if (request.getCategory() != null) spot.setCategory(request.getCategory());
        if (request.getCrewNote() != null) spot.setCrewNote(request.getCrewNote());
        if (request.getAddress() != null) spot.setAddress(request.getAddress());
        if (request.getLatitude() != null) spot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) spot.setLongitude(request.getLongitude());
        if (request.getArea() != null) spot.setArea(request.getArea());
        if (request.getNaverPlaceId() != null) spot.setNaverPlaceId(request.getNaverPlaceId());
        if (request.getKakaoPlaceId() != null) spot.setKakaoPlaceId(request.getKakaoPlaceId());
        if (request.getTags() != null) {
            spot.getTags().clear();
            spot.getTags().addAll(request.getTags());
        }
        if (request.getMedia() != null) {
            spot.getMedia().clear();
            spot.getMedia().addAll(request.getMedia());
        }
        if (request.getMediaItems() != null) {
            spot.getMediaItems().clear();
            addMediaItems(spot, request.getMediaItems());
        }

        // Reset to PENDING on user edit (re-review required)
        if ("user".equals(spot.getCreatorType())) {
            spot.setStatus(SpotStatus.PENDING);
            spot.setRejectionReason(null);
        }

        spotRepository.saveAndFlush(spot);
        Spot updated = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));
        PlaceInfo placeInfo = resolvePlaceInfo(updated);
        return SpotDetailResponse.from(updated, placeInfo, getS3BaseUrl());
    }

    /**
     * Spot 삭제 (soft delete)
     */
    @Transactional
    public void delete(String slug, String userId) {
        Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));
        verifyOwnership(spot.getCreatorId(), userId);
        spot.setIsActive(false);
        spotRepository.save(spot);
    }

    /**
     * Spot 대량 등록 (크루 배치 작업용)
     */
    @Transactional
    public List<SpotDetailResponse> bulkCreate(List<CreateSpotRequest> requests) {
        return requests.stream()
                .map(req -> create(req, null, "crew"))
                .toList();
    }

    private void verifyOwnership(String creatorId, String userId) {
        if (creatorId != null && !creatorId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 콘텐츠만 수정/삭제할 수 있습니다");
        }
    }

    /**
     * Location-based discovery — currentSpot + nextSpot recommendation
     */
    public DiscoverResponse discover(double lat, double lng, double radiusKm, UUID excludeSpotId) {
        String s3Base = getS3BaseUrl();

        // Step 1: Find nearest spot (expanding radius: 1km → 3km → 5km)
        Spot currentSpot = findNearestSpot(lat, lng, excludeSpotId);

        if (currentSpot == null) {
            // No nearby spots — return popular fallback
            return buildFallbackResponse();
        }

        PlaceInfo currentPlaceInfo = resolvePlaceInfo(currentSpot);
        double distanceFromUser = calculateDistance(lat, lng, currentSpot.getLatitude(), currentSpot.getLongitude());

        // Step 2: Find next spot — same area, different category, within 15min walk (~1.2km)
        Spot nextSpot = findNextSpot(currentSpot, excludeSpotId);
        DiscoverResponse.NextSpotInfo nextSpotInfo = null;

        if (nextSpot != null) {
            PlaceInfo nextPlaceInfo = resolvePlaceInfo(nextSpot);
            double distanceBetween = calculateDistance(
                    currentSpot.getLatitude(), currentSpot.getLongitude(),
                    nextSpot.getLatitude(), nextSpot.getLongitude());
            int walkingTime = (int) Math.ceil(distanceBetween / 67.0); // ~80m/min walking speed

            nextSpotInfo = DiscoverResponse.NextSpotInfo.builder()
                    .spot(SpotDetailResponse.from(nextSpot, nextPlaceInfo, s3Base))
                    .placeInfo(nextPlaceInfo)
                    .distanceFromCurrent(distanceBetween)
                    .walkingTime(walkingTime)
                    .build();
        }

        // Step 3: Nearby spots (up to 6, excluding current and next)
        List<SpotDetailResponse> nearbySpots = findNearbyExcluding(
                currentSpot.getLatitude(), currentSpot.getLongitude(),
                radiusKm, currentSpot.getId(), nextSpot != null ? nextSpot.getId() : null);

        // Step 4: Popular routes in the same area (up to 3)
        List<SpotLinePreviewResponse> popularSpotLines = findPopularSpotLines(currentSpot.getArea());

        return DiscoverResponse.builder()
                .currentSpot(DiscoverResponse.CurrentSpotInfo.builder()
                        .spot(SpotDetailResponse.from(currentSpot, currentPlaceInfo, s3Base))
                        .placeInfo(currentPlaceInfo)
                        .distanceFromUser(distanceFromUser)
                        .build())
                .nextSpot(nextSpotInfo)
                .nearbySpots(nearbySpots)
                .popularSpotLines(popularSpotLines)
                .area(currentSpot.getArea())
                .locationGranted(true)
                .build();
    }

    private Spot findNearestSpot(double lat, double lng, UUID excludeId) {
        double[] radii = {1.0, 3.0, 5.0};
        for (double radiusKm : radii) {
            double latDelta = radiusKm / 111.0;
            double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

            List<Spot> spots = spotRepository.findNearby(
                    lat - latDelta, lat + latDelta,
                    lng - lngDelta, lng + lngDelta);

            Spot nearest = spots.stream()
                    .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                    .min((a, b) -> {
                        double distA = calculateDistance(lat, lng, a.getLatitude(), a.getLongitude());
                        double distB = calculateDistance(lat, lng, b.getLatitude(), b.getLongitude());
                        return Double.compare(distA, distB);
                    })
                    .orElse(null);

            if (nearest != null) return nearest;
        }
        return null;
    }

    private Spot findNextSpot(Spot currentSpot, UUID excludeId) {
        // Search within ~1.2km (15min walk)
        double radiusKm = 1.2;
        double lat = currentSpot.getLatitude();
        double lng = currentSpot.getLongitude();
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Spot> candidates = spotRepository.findNearby(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta);

        // Filter: same area, different category, not current spot, not excluded, prioritize crewNote
        return candidates.stream()
                .filter(s -> !s.getId().equals(currentSpot.getId()))
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .filter(s -> s.getArea() != null && s.getArea().equals(currentSpot.getArea()))
                .filter(s -> s.getCategory() != currentSpot.getCategory())
                .sorted((a, b) -> {
                    // crewNote priority: spots with crewNote first
                    boolean aHasNote = a.getCrewNote() != null && !a.getCrewNote().isEmpty();
                    boolean bHasNote = b.getCrewNote() != null && !b.getCrewNote().isEmpty();
                    if (aHasNote != bHasNote) return bHasNote ? 1 : -1;
                    // then by viewsCount descending
                    return Integer.compare(b.getViewsCount(), a.getViewsCount());
                })
                .findFirst()
                .orElse(null);
    }

    private List<SpotDetailResponse> findNearbyExcluding(double lat, double lng, double radiusKm, UUID... excludeIds) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Spot> spots = spotRepository.findNearby(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta);

        java.util.Set<UUID> excludeSet = new java.util.HashSet<>();
        for (UUID id : excludeIds) {
            if (id != null) excludeSet.add(id);
        }

        String s3Base = getS3BaseUrl();
        return spots.stream()
                .filter(s -> !excludeSet.contains(s.getId()))
                .sorted((a, b) -> Integer.compare(b.getViewsCount(), a.getViewsCount()))
                .limit(6)
                .map(spot -> SpotDetailResponse.from(spot, null, s3Base))
                .toList();
    }

    private DiscoverResponse buildFallbackResponse() {
        String s3Base = getS3BaseUrl();

        // Return most popular spot overall
        List<Spot> popular = spotRepository.findByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(0, 7,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "viewsCount")))
                .getContent();

        if (popular.isEmpty()) {
            return DiscoverResponse.builder()
                    .locationGranted(false)
                    .nearbySpots(List.of())
                    .popularSpotLines(List.of())
                    .build();
        }

        Spot topSpot = popular.get(0);
        PlaceInfo topPlaceInfo = resolvePlaceInfo(topSpot);

        // Find next spot with different category
        Spot nextSpot = popular.stream()
                .filter(s -> s.getCategory() != topSpot.getCategory())
                .findFirst()
                .orElse(null);

        DiscoverResponse.NextSpotInfo nextSpotInfo = null;
        if (nextSpot != null) {
            PlaceInfo nextPlaceInfo = resolvePlaceInfo(nextSpot);
            double dist = calculateDistance(
                    topSpot.getLatitude(), topSpot.getLongitude(),
                    nextSpot.getLatitude(), nextSpot.getLongitude());
            nextSpotInfo = DiscoverResponse.NextSpotInfo.builder()
                    .spot(SpotDetailResponse.from(nextSpot, nextPlaceInfo, s3Base))
                    .placeInfo(nextPlaceInfo)
                    .distanceFromCurrent(dist)
                    .walkingTime((int) Math.ceil(dist / 67.0))
                    .build();
        }

        List<SpotDetailResponse> nearby = popular.stream()
                .skip(nextSpot != null ? 2 : 1)
                .limit(6)
                .map(s -> SpotDetailResponse.from(s, null, s3Base))
                .toList();

        return DiscoverResponse.builder()
                .currentSpot(DiscoverResponse.CurrentSpotInfo.builder()
                        .spot(SpotDetailResponse.from(topSpot, topPlaceInfo, s3Base))
                        .placeInfo(topPlaceInfo)
                        .distanceFromUser(0)
                        .build())
                .nextSpot(nextSpotInfo)
                .nearbySpots(nearby)
                .popularSpotLines(findPopularSpotLines(topSpot.getArea()))
                .area(topSpot.getArea())
                .locationGranted(false)
                .build();
    }

    private List<SpotLinePreviewResponse> findPopularSpotLines(String area) {
        if (area == null) return List.of();
        return spotLineRepository.findByAreaLikeAndPopular(
                        area, org.springframework.data.domain.PageRequest.of(0, 3))
                .getContent()
                .stream()
                .map(SpotLinePreviewResponse::from)
                .toList();
    }

    /**
     * Haversine formula — distance between two coordinates in meters
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ---- Approval Workflow ----

    /**
     * 내 Spot 목록 조회 (상태 필터 지원)
     */
    public Page<SpotDetailResponse> getMySpots(String userId, SpotStatus status, Pageable pageable) {
        Page<Spot> spots;
        if (status != null) {
            spots = spotRepository.findByCreatorIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            spots = spotRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable);
        }
        String s3BaseUrl = getS3BaseUrl();
        return spots.map(spot -> SpotDetailResponse.from(spot, null, s3BaseUrl));
    }

    /**
     * 검토 대기 Spot 목록 (어드민)
     */
    public Page<SpotDetailResponse> getPendingSpots(Pageable pageable) {
        Page<Spot> spots = spotRepository.findByStatusOrderByCreatedAtAsc(SpotStatus.PENDING, pageable);
        String s3BaseUrl = getS3BaseUrl();
        return spots.map(spot -> SpotDetailResponse.from(spot, null, s3BaseUrl));
    }

    /**
     * Spot 승인 (어드민)
     */
    @Transactional
    public SpotDetailResponse approve(String slug, String adminId) {
        Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));
        spot.setStatus(SpotStatus.APPROVED);
        spot.setRejectionReason(null);
        spot.setReviewedAt(LocalDateTime.now());
        spot.setReviewedBy(adminId);
        spotRepository.save(spot);

        if (spot.getCreatorId() != null) {
            notificationService.create(adminId, spot.getCreatorId(),
                NotificationType.SPOT_APPROVED, "SPOT",
                spot.getId().toString(), spot.getSlug());
        }

        return SpotDetailResponse.from(spot, null, getS3BaseUrl());
    }

    /**
     * Spot 반려 (어드민)
     */
    @Transactional
    public SpotDetailResponse reject(String slug, String reason, String adminId) {
        Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));
        spot.setStatus(SpotStatus.REJECTED);
        spot.setRejectionReason(reason);
        spot.setReviewedAt(LocalDateTime.now());
        spot.setReviewedBy(adminId);
        spotRepository.save(spot);

        if (spot.getCreatorId() != null) {
            notificationService.create(adminId, spot.getCreatorId(),
                NotificationType.SPOT_REJECTED, "SPOT",
                spot.getId().toString(), spot.getSlug());
        }

        return SpotDetailResponse.from(spot, null, getS3BaseUrl());
    }

    /**
     * 검토 대기 Spot 수 (뱃지용)
     */
    public long countPending() {
        return spotRepository.countByStatusAndIsActiveTrue(SpotStatus.PENDING);
    }

    // ---- Private helpers ----

    private PlaceInfo resolvePlaceInfo(Spot spot) {
        // 카카오 우선, 네이버 폴백
        if (spot.getKakaoPlaceId() != null && !spot.getKakaoPlaceId().isEmpty()) {
            PlaceInfo info = placeApiService.getPlaceDetail("kakao", spot.getKakaoPlaceId());
            if (info != null) return info;
        }
        if (spot.getNaverPlaceId() != null && !spot.getNaverPlaceId().isEmpty()) {
            return placeApiService.getPlaceDetail("naver", spot.getNaverPlaceId());
        }
        return null; // graceful: Place API 정보 없음
    }

    private void addMediaItems(Spot spot, List<MediaItemRequest> items) {
        for (int i = 0; i < items.size(); i++) {
            MediaItemRequest item = items.get(i);
            SpotMedia media = SpotMedia.builder()
                    .spot(spot)
                    .s3Key(item.getS3Key())
                    .mediaType(item.getMediaType())
                    .thumbnailS3Key(item.getThumbnailS3Key())
                    .durationSec(item.getDurationSec())
                    .displayOrder(item.getDisplayOrder() != null ? item.getDisplayOrder() : i)
                    .fileSizeBytes(item.getFileSizeBytes())
                    .mimeType(item.getMimeType())
                    .build();
            spot.getMediaItems().add(media);
        }
    }

    private String getS3BaseUrl() {
        return s3Service.getPublicUrl("").replaceAll("/$", "");
    }

    public List<SlugResponse> getAllSlugs() {
        return spotRepository.findAllActiveSlugs().stream()
                .map(s -> SlugResponse.builder()
                        .slug(s.getSlug())
                        .updatedAt(s.getUpdatedAt())
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
        while (spotRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
