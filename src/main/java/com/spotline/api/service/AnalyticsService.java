package com.spotline.api.service;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.CommentRepository;
import com.spotline.api.domain.repository.ContentReportRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.response.DailyContentTrendResponse;
import com.spotline.api.dto.response.PlatformStatsResponse;
import com.spotline.api.dto.response.PopularContentResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
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

    private Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }
}
