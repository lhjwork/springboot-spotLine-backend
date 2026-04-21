package com.spotline.api.service;

import com.spotline.api.domain.entity.Share;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.enums.ShareChannel;
import com.spotline.api.domain.repository.ShareRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.ShareRequest;
import com.spotline.api.dto.response.DailyShareCount;
import com.spotline.api.dto.response.ShareStatsResponse;
import com.spotline.api.dto.response.TopSharedContentResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    private final ShareRepository shareRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;

    @Transactional
    public int trackShare(ShareRequest request, String sharerId) {
        Share share = Share.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .channel(request.getChannel())
                .sharerId(sharerId)
                .referrerId(request.getReferrerId())
                .build();
        shareRepository.save(share);

        switch (request.getTargetType().toUpperCase()) {
            case "SPOT" -> {
                Spot spot = spotRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Spot", request.getTargetId().toString()));
                spot.setSharesCount(spot.getSharesCount() + 1);
                spotRepository.save(spot);
                return spot.getSharesCount();
            }
            case "SPOTLINE" -> {
                SpotLine spotLine = spotLineRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("SpotLine", request.getTargetId().toString()));
                spotLine.setSharesCount(spotLine.getSharesCount() + 1);
                spotLineRepository.save(spotLine);
                return spotLine.getSharesCount();
            }
            default -> log.warn("알 수 없는 공유 대상 타입: {}", request.getTargetType());
        }

        log.info("공유 추적 완료: {} {} via {}", request.getTargetType(), request.getTargetId(), request.getChannel());
        return 0;
    }

    @Transactional(readOnly = true)
    public ShareStatsResponse getShareStats(String period, String targetType) {
        LocalDateTime since = parsePeriod(period);
        List<Object[]> channelCounts = shareRepository.countByChannelSince(since, targetType);
        List<Object[]> dailyTrend = shareRepository.dailyTrendSince(since);

        Map<ShareChannel, Long> channelBreakdown = new EnumMap<>(ShareChannel.class);
        long totalShares = 0;
        for (Object[] row : channelCounts) {
            ShareChannel channel = (ShareChannel) row[0];
            Long count = (Long) row[1];
            channelBreakdown.put(channel, count);
            totalShares += count;
        }

        List<DailyShareCount> dailyList = dailyTrend.stream()
                .map(row -> new DailyShareCount((LocalDate) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        return ShareStatsResponse.builder()
                .totalShares(totalShares)
                .channelBreakdown(channelBreakdown)
                .dailyTrend(dailyList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopSharedContentResponse> getTopShared(String period, String targetType, int limit) {
        LocalDateTime since = parsePeriod(period);
        List<Object[]> results = shareRepository.topSharedContent(
                since, targetType, PageRequest.of(0, limit));

        return results.stream()
                .map(row -> {
                    UUID targetId = (UUID) row[0];
                    String type = (String) row[1];
                    Long count = (Long) row[2];

                    String title = "";
                    String slug = "";
                    if ("SPOT".equalsIgnoreCase(type)) {
                        var spotOpt = spotRepository.findById(targetId);
                        if (spotOpt.isPresent()) {
                            title = spotOpt.get().getTitle();
                            slug = spotOpt.get().getSlug();
                        }
                    } else if ("SPOTLINE".equalsIgnoreCase(type)) {
                        var slOpt = spotLineRepository.findById(targetId);
                        if (slOpt.isPresent()) {
                            title = slOpt.get().getTitle();
                            slug = slOpt.get().getSlug();
                        }
                    }

                    return TopSharedContentResponse.builder()
                            .targetId(targetId)
                            .targetType(type)
                            .title(title)
                            .slug(slug)
                            .shareCount(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime parsePeriod(String period) {
        if (period == null || period.equals("30d")) return LocalDateTime.now().minusDays(30);
        return switch (period) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "90d" -> LocalDateTime.now().minusDays(90);
            default -> LocalDateTime.now().minusDays(30);
        };
    }
}
