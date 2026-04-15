package com.spotline.api.service;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotVisit;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.SpotVisitRepository;
import com.spotline.api.dto.request.CheckinRequest;
import com.spotline.api.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckinService {

    private static final double CHECKIN_RADIUS_METERS = 500.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final String[] DAY_NAMES = {"일", "월", "화", "수", "목", "금", "토"};

    private final SpotRepository spotRepository;
    private final SpotVisitRepository spotVisitRepository;

    public CheckinResponse checkin(String userId, UUID spotId, CheckinRequest request) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spot을 찾을 수 없습니다"));

        // 24시간 중복 체크
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        if (spotVisitRepository.existsByUserIdAndSpotAndCreatedAtAfter(userId, spot, oneDayAgo)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "24시간 내 이미 체크인하였습니다");
        }

        // GPS 검증
        boolean verified = false;
        if (request.getLatitude() != null && request.getLongitude() != null
                && spot.getLatitude() != null && spot.getLongitude() != null) {
            double distance = haversine(
                request.getLatitude(), request.getLongitude(),
                spot.getLatitude(), spot.getLongitude()
            );
            verified = distance <= CHECKIN_RADIUS_METERS;
        }

        SpotVisit visit = SpotVisit.builder()
            .userId(userId)
            .spot(spot)
            .memo(request.getMemo())
            .verified(verified)
            .build();
        spotVisitRepository.save(visit);

        // visitedCount 증가
        spot.setVisitedCount(spot.getVisitedCount() + 1);
        spotRepository.save(spot);

        long myCount = spotVisitRepository.countByUserIdAndSpot(userId, spot);

        return CheckinResponse.builder()
            .id(visit.getId())
            .spotId(spotId)
            .verified(verified)
            .memo(visit.getMemo())
            .visitedCount(spot.getVisitedCount())
            .myCheckinCount(myCount)
            .createdAt(visit.getCreatedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public Page<CheckinListResponse> getSpotCheckins(UUID spotId, Pageable pageable) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return spotVisitRepository.findBySpotOrderByCreatedAtDesc(spot, pageable)
            .map(sv -> CheckinListResponse.builder()
                .id(sv.getId())
                .userId(sv.getUserId())
                .memo(sv.getMemo())
                .verified(sv.getVerified())
                .createdAt(sv.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<CheckinListResponse> getUserCheckins(String userId, Pageable pageable) {
        return spotVisitRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(sv -> CheckinListResponse.builder()
                .id(sv.getId())
                .userId(sv.getUserId())
                .memo(sv.getMemo())
                .verified(sv.getVerified())
                .createdAt(sv.getCreatedAt())
                .spotId(sv.getSpot().getId())
                .spotTitle(sv.getSpot().getTitle())
                .spotSlug(sv.getSpot().getSlug())
                .spotThumbnail(sv.getSpot().getMedia().isEmpty() ? null : sv.getSpot().getMedia().get(0))
                .build());
    }

    // ==================== Admin ====================

    @Transactional(readOnly = true)
    public CheckinStatsResponse getCheckinStats(LocalDateTime from, LocalDateTime to) {
        long total = spotVisitRepository.countByCreatedAtBetween(from, to);
        long verified = spotVisitRepository.countByVerifiedTrueAndCreatedAtBetween(from, to);
        long uniqueUsers = spotVisitRepository.countDistinctUsersByCreatedAtBetween(from, to);
        long uniqueSpots = spotVisitRepository.countDistinctSpotsByCreatedAtBetween(from, to);

        return CheckinStatsResponse.builder()
            .totalCheckins(total)
            .verifiedCheckins(verified)
            .uniqueUsers(uniqueUsers)
            .uniqueSpots(uniqueSpots)
            .verificationRate(total > 0 ? Math.round((double) verified / total * 10000) / 100.0 : 0.0)
            .avgCheckinsPerUser(uniqueUsers > 0 ? Math.round((double) total / uniqueUsers * 100) / 100.0 : 0.0)
            .build();
    }

    @Transactional(readOnly = true)
    public TopSpotCheckinResponse getTopSpots(LocalDateTime from, LocalDateTime to, int limit) {
        List<Object[]> rows = spotVisitRepository.findTopSpotsByCheckins(from, to,
            org.springframework.data.domain.PageRequest.of(0, limit));

        List<TopSpotCheckinResponse.TopSpotItem> items = rows.stream().map(row -> {
            long totalCheckins = ((Number) row[3]).longValue();
            long verifiedCnt = ((Number) row[5]).longValue();
            return TopSpotCheckinResponse.TopSpotItem.builder()
                .spotId((UUID) row[0])
                .spotTitle((String) row[1])
                .area((String) row[2])
                .totalCheckins(totalCheckins)
                .uniqueVisitors(((Number) row[4]).longValue())
                .verifiedRate(totalCheckins > 0 ? Math.round((double) verifiedCnt / totalCheckins * 10000) / 100.0 : 0.0)
                .build();
        }).toList();

        return TopSpotCheckinResponse.builder().spots(items).build();
    }

    @Transactional(readOnly = true)
    public CheckinPatternResponse getCheckinPattern(LocalDateTime from, LocalDateTime to) {
        List<Object[]> hourlyRows = spotVisitRepository.findHourlyPattern(from, to);
        List<CheckinPatternResponse.HourlyItem> hourly = hourlyRows.stream()
            .map(row -> new CheckinPatternResponse.HourlyItem(
                ((Number) row[0]).intValue(),
                ((Number) row[1]).longValue()
            )).toList();

        List<Object[]> dailyRows = spotVisitRepository.findDailyPattern(from, to);
        List<CheckinPatternResponse.DailyItem> daily = dailyRows.stream()
            .map(row -> {
                int dow = ((Number) row[0]).intValue();
                return new CheckinPatternResponse.DailyItem(
                    DAY_NAMES[dow],
                    ((Number) row[1]).longValue()
                );
            }).toList();

        return CheckinPatternResponse.builder()
            .hourly(hourly)
            .daily(daily)
            .build();
    }

    // ==================== Haversine ====================

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
