package com.spotline.api.service;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.CommentRepository;
import com.spotline.api.domain.repository.ContentReportRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.response.*;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final ContentReportRepository contentReportRepository;

    public PlatformStatsResponse getPlatformStats() {
        return PlatformStatsResponse.builder()
                .totalSpots(spotRepository.countByIsActiveTrue())
                .totalSpotLines(spotLineRepository.countByIsActiveTrue())
                .totalComments(commentRepository.count())
                .totalReports(contentReportRepository.count())
                .totalSpotViews(spotRepository.sumViewsCountByIsActiveTrue())
                .totalSpotLineViews(spotLineRepository.sumViewsCountByIsActiveTrue())
                .build();
    }

    public List<PopularContentResponse> getPopularSpots() {
        return spotRepository.findTop10ByIsActiveTrueOrderByViewsCountDesc()
                .stream()
                .map(s -> PopularContentResponse.builder()
                        .id(s.getId())
                        .slug(s.getSlug())
                        .title(s.getTitle())
                        .label(s.getArea())
                        .viewsCount(s.getViewsCount())
                        .commentsCount(s.getCommentsCount())
                        .build())
                .toList();
    }

    public List<PopularContentResponse> getPopularSpotLines() {
        return spotLineRepository.findTop10ByIsActiveTrueOrderByViewsCountDesc()
                .stream()
                .map(r -> PopularContentResponse.builder()
                        .id(r.getId())
                        .slug(r.getSlug())
                        .title(r.getTitle())
                        .label(r.getTheme() != null ? r.getTheme().name() : "")
                        .viewsCount(r.getViewsCount())
                        .commentsCount(r.getCommentsCount())
                        .build())
                .toList();
    }

    public List<DailyContentTrendResponse> getDailyTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<LocalDate, Long> spotMap = toDateMap(spotRepository.countDailyCreatedSince(since));
        Map<LocalDate, Long> spotLineMap = toDateMap(spotLineRepository.countDailyCreatedSince(since));

        List<DailyContentTrendResponse> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(DailyContentTrendResponse.builder()
                    .date(date)
                    .spotCount(spotMap.getOrDefault(date, 0L))
                    .spotLineCount(spotLineMap.getOrDefault(date, 0L))
                    .build());
        }
        return result;
    }

    @Transactional
    public void incrementSpotView(UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Spot", spotId.toString()));
        spot.setViewsCount(spot.getViewsCount() + 1);
        spotRepository.save(spot);
    }

    @Transactional
    public void incrementSpotLineView(UUID routeId) {
        SpotLine spotLine = spotLineRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("SpotLine", routeId.toString()));
        spotLine.setViewsCount(spotLine.getViewsCount() + 1);
        spotLineRepository.save(spotLine);
    }

    @Transactional
    public void incrementBlogView(UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", blogId.toString()));
        blog.setViewsCount(blog.getViewsCount() + 1);
        blogRepository.save(blog);
    }

    // ---- BI Analytics ----

    @Cacheable(value = "analyticsContentPerf", key = "#from + '-' + #to + '-' + #type + '-' + #sort")
    public List<ContentPerformanceResponse> getContentPerformance(LocalDate from, LocalDate to, String type, String sort, int limit) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);
        var pageable = PageRequest.of(0, limit);

        if ("spotline".equals(type)) {
            var items = switch (sort) {
                case "likes" -> spotLineRepository.findActiveByDateRangeOrderByLikes(fromDt, toDt, pageable);
                case "saves" -> spotLineRepository.findActiveByDateRangeOrderBySaves(fromDt, toDt, pageable);
                case "comments" -> spotLineRepository.findActiveByDateRangeOrderByComments(fromDt, toDt, pageable);
                default -> spotLineRepository.findActiveByDateRangeOrderByViews(fromDt, toDt, pageable);
            };
            return items.stream().map(sl -> ContentPerformanceResponse.builder()
                    .id(sl.getId()).slug(sl.getSlug()).title(sl.getTitle())
                    .area(sl.getArea()).creatorName(sl.getCreatorName())
                    .viewsCount(sl.getViewsCount()).likesCount(sl.getLikesCount())
                    .savesCount(sl.getSavesCount()).commentsCount(sl.getCommentsCount())
                    .createdAt(sl.getCreatedAt()).build()).toList();
        }

        var items = switch (sort) {
            case "likes" -> spotRepository.findActiveByDateRangeOrderByLikes(fromDt, toDt, pageable);
            case "saves" -> spotRepository.findActiveByDateRangeOrderBySaves(fromDt, toDt, pageable);
            case "comments" -> spotRepository.findActiveByDateRangeOrderByComments(fromDt, toDt, pageable);
            default -> spotRepository.findActiveByDateRangeOrderByViews(fromDt, toDt, pageable);
        };
        return items.stream().map(s -> ContentPerformanceResponse.builder()
                .id(s.getId()).slug(s.getSlug()).title(s.getTitle())
                .area(s.getArea()).creatorName(s.getCreatorName())
                .viewsCount(s.getViewsCount()).likesCount(s.getLikesCount())
                .savesCount(s.getSavesCount()).commentsCount(s.getCommentsCount())
                .createdAt(s.getCreatedAt()).build()).toList();
    }

    @Cacheable(value = "analyticsCreatorProd", key = "#from + '-' + #to")
    public List<CreatorProductivityResponse> getCreatorProductivity(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        Map<String, long[]> creatorMap = new LinkedHashMap<>();
        Map<String, String[]> creatorInfo = new HashMap<>();

        for (Object[] row : spotRepository.aggregateByCreator(fromDt, toDt)) {
            String cid = (String) row[0];
            creatorInfo.put(cid, new String[]{(String) row[1], (String) row[2]});
            long[] stats = creatorMap.computeIfAbsent(cid, k -> new long[4]);
            stats[0] += (Long) row[3]; // spotCount
            stats[2] += (Long) row[4]; // views
            stats[3] += (Long) row[5]; // likes
        }
        for (Object[] row : spotLineRepository.aggregateByCreator(fromDt, toDt)) {
            String cid = (String) row[0];
            creatorInfo.putIfAbsent(cid, new String[]{(String) row[1], (String) row[2]});
            long[] stats = creatorMap.computeIfAbsent(cid, k -> new long[4]);
            stats[1] += (Long) row[3]; // spotLineCount
            stats[2] += (Long) row[4]; // views
            stats[3] += (Long) row[5]; // likes
        }

        return creatorMap.entrySet().stream().map(e -> {
            String[] info = creatorInfo.get(e.getKey());
            long[] s = e.getValue();
            long totalContent = s[0] + s[1];
            return CreatorProductivityResponse.builder()
                    .creatorId(e.getKey())
                    .creatorName(info[0]).creatorType(info[1])
                    .spotCount(s[0]).spotLineCount(s[1])
                    .totalViews(s[2]).totalLikes(s[3])
                    .avgViewsPerContent(totalContent > 0 ? (double) s[2] / totalContent : 0)
                    .build();
        }).sorted(Comparator.comparingLong(CreatorProductivityResponse::getTotalViews).reversed()).toList();
    }

    @Cacheable(value = "analyticsAreaPerf", key = "#from + '-' + #to")
    public List<AreaPerformanceResponse> getAreaPerformance(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        Map<String, long[]> areaMap = new LinkedHashMap<>();

        for (Object[] row : spotRepository.aggregateByArea(fromDt, toDt)) {
            String area = (String) row[0];
            long[] stats = areaMap.computeIfAbsent(area, k -> new long[4]);
            stats[0] += (Long) row[1]; // spotCount
            stats[2] += (Long) row[2]; // views
            stats[3] += (Long) row[3]; // likes
        }
        for (Object[] row : spotLineRepository.aggregateByArea(fromDt, toDt)) {
            String area = (String) row[0];
            long[] stats = areaMap.computeIfAbsent(area, k -> new long[4]);
            stats[1] += (Long) row[1]; // spotLineCount
            stats[2] += (Long) row[2]; // views
            stats[3] += (Long) row[3]; // likes
        }

        return areaMap.entrySet().stream().map(e -> {
            long[] s = e.getValue();
            long totalSpots = s[0];
            return AreaPerformanceResponse.builder()
                    .area(e.getKey())
                    .spotCount(s[0]).spotLineCount(s[1])
                    .totalViews(s[2]).totalLikes(s[3])
                    .avgViewsPerSpot(totalSpots > 0 ? (double) s[2] / totalSpots : 0)
                    .build();
        }).sorted(Comparator.comparingLong(AreaPerformanceResponse::getTotalViews).reversed()).toList();
    }

    @Cacheable(value = "analyticsPeriodComp", key = "#from + '-' + #to")
    public PeriodComparisonResponse getPeriodComparison(LocalDate from, LocalDate to) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        LocalDate prevFrom = from.minusDays(daysBetween + 1);
        LocalDate prevTo = from.minusDays(1);

        LocalDateTime curFromDt = from.atStartOfDay();
        LocalDateTime curToDt = to.atTime(23, 59, 59);
        LocalDateTime prevFromDt = prevFrom.atStartOfDay();
        LocalDateTime prevToDt = prevTo.atTime(23, 59, 59);

        Object[] curSpot = spotRepository.aggregateStats(curFromDt, curToDt);
        Object[] curSl = spotLineRepository.aggregateStats(curFromDt, curToDt);
        Object[] prevSpot = spotRepository.aggregateStats(prevFromDt, prevToDt);
        Object[] prevSl = spotLineRepository.aggregateStats(prevFromDt, prevToDt);

        long cSpots = (Long) curSpot[0];
        long cSpotLines = (Long) curSl[0];
        long cViews = (Long) curSpot[1] + (Long) curSl[1];
        long cLikes = (Long) curSpot[2] + (Long) curSl[2];
        long pSpots = (Long) prevSpot[0];
        long pSpotLines = (Long) prevSl[0];
        long pViews = (Long) prevSpot[1] + (Long) prevSl[1];
        long pLikes = (Long) prevSpot[2] + (Long) prevSl[2];

        return PeriodComparisonResponse.builder()
                .currentSpots(cSpots).currentSpotLines(cSpotLines)
                .currentViews(cViews).currentLikes(cLikes)
                .previousSpots(pSpots).previousSpotLines(pSpotLines)
                .previousViews(pViews).previousLikes(pLikes)
                .spotsChangeRate(calcChangeRate(cSpots, pSpots))
                .spotLinesChangeRate(calcChangeRate(cSpotLines, pSpotLines))
                .viewsChangeRate(calcChangeRate(cViews, pViews))
                .likesChangeRate(calcChangeRate(cLikes, pLikes))
                .build();
    }

    private double calcChangeRate(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((double) (current - previous) / previous * 10000) / 100.0;
    }

    private Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }
}
