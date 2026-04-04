package com.spotline.api.service;

import com.github.slugify.Slugify;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.SpotLineSpot;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.domain.enums.SpotLineTheme;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.infrastructure.s3.S3Service;
import com.spotline.api.dto.request.CreateSpotLineRequest;
import com.spotline.api.dto.request.UpdateSpotLineRequest;
import com.spotline.api.dto.response.SpotLineDetailResponse;
import com.spotline.api.dto.response.SlugResponse;
import com.spotline.api.dto.response.SpotLinePreviewResponse;
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
public class SpotLineService {

    private final SpotLineRepository spotLineRepository;
    private final SpotRepository spotRepository;
    private final S3Service s3Service;
    private final Slugify slugify = Slugify.builder().transliterator(true).build();

    public SpotLine getBySlug(String slug) {
        return spotLineRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("SpotLine", slug));
    }

    public SpotLineDetailResponse getDetailBySlug(String slug) {
        SpotLine spotLine = getBySlug(slug);
        return SpotLineDetailResponse.from(spotLine);
    }

    public Page<SpotLinePreviewResponse> getPopularPreviews(
            String area, String theme, String keyword, FeedSort sort, Pageable pageable) {
        FeedSort effectiveSort = (sort != null) ? sort : FeedSort.POPULAR;
        Page<SpotLine> spotLines;
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (effectiveSort == FeedSort.NEWEST) {
            spotLines = hasKeyword
                ? getNewestWithKeyword(area, theme, keyword, pageable)
                : getNewest(area, theme, pageable);
        } else {
            spotLines = hasKeyword
                ? getPopularWithKeyword(area, theme, keyword, pageable)
                : getPopular(area, theme, pageable);
        }

        String s3BaseUrl = getS3BaseUrl();
        return spotLines.map(sl -> SpotLinePreviewResponse.from(sl, s3BaseUrl));
    }

    private Page<SpotLine> getNewest(String area, String theme, Pageable pageable) {
        if (area != null && theme != null) {
            return spotLineRepository.findByAreaLikeAndThemeAndNewest(
                    area, SpotLineTheme.valueOf(theme.toUpperCase()), pageable);
        } else if (area != null) {
            return spotLineRepository.findByAreaLikeAndNewest(area, pageable);
        } else if (theme != null) {
            return spotLineRepository.findByThemeAndIsActiveTrueOrderByCreatedAtDesc(
                    SpotLineTheme.valueOf(theme.toUpperCase()), pageable);
        }
        return spotLineRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
    }

    private Page<SpotLine> getPopularWithKeyword(String area, String theme, String keyword, Pageable pageable) {
        if (area != null && theme != null) {
            return spotLineRepository.findByAreaLikeAndThemeAndKeywordAndPopular(
                    area, SpotLineTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return spotLineRepository.findByAreaLikeAndKeywordAndPopular(area, keyword, pageable);
        } else if (theme != null) {
            return spotLineRepository.findByThemeAndKeywordAndPopular(
                    SpotLineTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        }
        return spotLineRepository.findByKeywordAndPopular(keyword, pageable);
    }

    private Page<SpotLine> getNewestWithKeyword(String area, String theme, String keyword, Pageable pageable) {
        if (area != null && theme != null) {
            return spotLineRepository.findByAreaLikeAndThemeAndKeywordAndNewest(
                    area, SpotLineTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        } else if (area != null) {
            return spotLineRepository.findByAreaLikeAndKeywordAndNewest(area, keyword, pageable);
        } else if (theme != null) {
            return spotLineRepository.findByThemeAndKeywordAndNewest(
                    SpotLineTheme.valueOf(theme.toUpperCase()), keyword, pageable);
        }
        return spotLineRepository.findByKeywordAndNewest(keyword, pageable);
    }

    @Transactional
    public SpotLineDetailResponse createAndReturn(CreateSpotLineRequest request, String userId, String creatorType) {
        SpotLine spotLine = create(request, userId, creatorType);
        return SpotLineDetailResponse.from(spotLine);
    }

    public Page<SpotLine> getPopular(String area, String theme, Pageable pageable) {
        if (area != null && theme != null) {
            return spotLineRepository.findByAreaLikeAndThemeAndPopular(
                    area, com.spotline.api.domain.enums.SpotLineTheme.valueOf(theme.toUpperCase()), pageable);
        } else if (area != null) {
            return spotLineRepository.findByAreaLikeAndPopular(area, pageable);
        } else if (theme != null) {
            return spotLineRepository.findByThemeAndIsActiveTrueOrderByLikesCountDesc(
                    com.spotline.api.domain.enums.SpotLineTheme.valueOf(theme.toUpperCase()), pageable);
        }
        return spotLineRepository.findByIsActiveTrueOrderByLikesCountDesc(pageable);
    }

    @Transactional
    public SpotLine create(CreateSpotLineRequest request, String userId, String creatorType) {
        String slug = generateUniqueSlug(request.getTitle());

        SpotLine spotLine = SpotLine.builder()
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
        if (request.getParentSpotLineId() != null) {
            SpotLine parent = spotLineRepository.findById(request.getParentSpotLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("SpotLine", request.getParentSpotLineId().toString()));
            spotLine.setParentSpotLine(parent);
        }

        // SpotLineSpot 추가
        int totalDuration = 0;
        int totalDistance = 0;
        for (int i = 0; i < request.getSpots().size(); i++) {
            CreateSpotLineRequest.SpotLineSpotRequest spotReq = request.getSpots().get(i);
            Spot spot = spotRepository.findById(spotReq.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Spot", spotReq.getSpotId().toString()));

            SpotLineSpot spotLineSpot = SpotLineSpot.builder()
                    .spotLine(spotLine)
                    .spot(spot)
                    .spotOrder(spotReq.getOrder() != null ? spotReq.getOrder() : i + 1)
                    .suggestedTime(spotReq.getSuggestedTime())
                    .stayDuration(spotReq.getStayDuration())
                    .walkingTimeToNext(spotReq.getWalkingTimeToNext())
                    .distanceToNext(spotReq.getDistanceToNext())
                    .transitionNote(spotReq.getTransitionNote())
                    .build();

            spotLine.getSpots().add(spotLineSpot);

            if (spotReq.getStayDuration() != null) totalDuration += spotReq.getStayDuration();
            if (spotReq.getWalkingTimeToNext() != null) totalDuration += spotReq.getWalkingTimeToNext();
            if (spotReq.getDistanceToNext() != null) totalDistance += spotReq.getDistanceToNext();
        }

        spotLine.setTotalDuration(totalDuration);
        spotLine.setTotalDistance(totalDistance);

        return spotLineRepository.save(spotLine);
    }

    @Transactional
    public SpotLineDetailResponse update(String slug, UpdateSpotLineRequest request, String userId) {
        SpotLine spotLine = getBySlug(slug);
        verifyOwnership(spotLine.getCreatorId(), userId);

        if (request.getTitle() != null) spotLine.setTitle(request.getTitle());
        if (request.getDescription() != null) spotLine.setDescription(request.getDescription());
        if (request.getTheme() != null) spotLine.setTheme(request.getTheme());
        if (request.getArea() != null) spotLine.setArea(request.getArea());

        if (request.getSpots() != null) {
            spotLine.getSpots().clear(); // orphanRemoval 자동 삭제

            int totalDuration = 0;
            int totalDistance = 0;
            for (int i = 0; i < request.getSpots().size(); i++) {
                CreateSpotLineRequest.SpotLineSpotRequest spotReq = request.getSpots().get(i);
                Spot spot = spotRepository.findById(spotReq.getSpotId())
                        .orElseThrow(() -> new ResourceNotFoundException("Spot", spotReq.getSpotId().toString()));

                SpotLineSpot spotLineSpot = SpotLineSpot.builder()
                        .spotLine(spotLine)
                        .spot(spot)
                        .spotOrder(spotReq.getOrder() != null ? spotReq.getOrder() : i + 1)
                        .suggestedTime(spotReq.getSuggestedTime())
                        .stayDuration(spotReq.getStayDuration())
                        .walkingTimeToNext(spotReq.getWalkingTimeToNext())
                        .distanceToNext(spotReq.getDistanceToNext())
                        .transitionNote(spotReq.getTransitionNote())
                        .build();

                spotLine.getSpots().add(spotLineSpot);
                if (spotReq.getStayDuration() != null) totalDuration += spotReq.getStayDuration();
                if (spotReq.getWalkingTimeToNext() != null) totalDuration += spotReq.getWalkingTimeToNext();
                if (spotReq.getDistanceToNext() != null) totalDistance += spotReq.getDistanceToNext();
            }
            spotLine.setTotalDuration(totalDuration);
            spotLine.setTotalDistance(totalDistance);
        }

        return SpotLineDetailResponse.from(spotLineRepository.save(spotLine));
    }

    @Transactional
    public void delete(String slug, String userId) {
        SpotLine spotLine = getBySlug(slug);
        verifyOwnership(spotLine.getCreatorId(), userId);
        spotLine.setIsActive(false);
        spotLineRepository.save(spotLine);
    }

    private void verifyOwnership(String creatorId, String userId) {
        if (creatorId != null && !creatorId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 콘텐츠만 수정/삭제할 수 있습니다");
        }
    }

    public List<SlugResponse> getAllSlugs() {
        return spotLineRepository.findAllActiveSlugs().stream()
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
        while (spotLineRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
