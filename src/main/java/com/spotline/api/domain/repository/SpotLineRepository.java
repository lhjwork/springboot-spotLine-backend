package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.enums.SpotLineTheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotLineRepository extends JpaRepository<SpotLine, UUID> {

    Optional<SpotLine> findBySlugAndIsActiveTrue(String slug);

    // ---- Popular (likesCount DESC) ----
    Page<SpotLine> findByIsActiveTrueOrderByLikesCountDesc(Pageable pageable);

    Page<SpotLine> findByThemeAndIsActiveTrueOrderByLikesCountDesc(SpotLineTheme theme, Pageable pageable);

    // ---- Area LIKE 매칭 (연남 → 연남, 연남동 모두 매칭) ----
    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true AND s.area LIKE %:area% ORDER BY s.likesCount DESC")
    Page<SpotLine> findByAreaLikeAndPopular(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true AND s.area LIKE %:area% AND s.theme = :theme ORDER BY s.likesCount DESC")
    Page<SpotLine> findByAreaLikeAndThemeAndPopular(@Param("area") String area, @Param("theme") SpotLineTheme theme, Pageable pageable);

    // ---- Newest (createdAt DESC) ----
    Page<SpotLine> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<SpotLine> findByThemeAndIsActiveTrueOrderByCreatedAtDesc(SpotLineTheme theme, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true AND s.area LIKE %:area% ORDER BY s.createdAt DESC")
    Page<SpotLine> findByAreaLikeAndNewest(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true AND s.area LIKE %:area% AND s.theme = :theme ORDER BY s.createdAt DESC")
    Page<SpotLine> findByAreaLikeAndThemeAndNewest(@Param("area") String area, @Param("theme") SpotLineTheme theme, Pageable pageable);

    // ---- Keyword 검색 (title OR description LIKE) ----

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.likesCount DESC")
    Page<SpotLine> findByKeywordAndPopular(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<SpotLine> findByKeywordAndNewest(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.likesCount DESC")
    Page<SpotLine> findByAreaLikeAndKeywordAndPopular(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<SpotLine> findByAreaLikeAndKeywordAndNewest(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.theme = :theme " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.likesCount DESC")
    Page<SpotLine> findByThemeAndKeywordAndPopular(
        @Param("theme") SpotLineTheme theme, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.theme = :theme " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<SpotLine> findByThemeAndKeywordAndNewest(
        @Param("theme") SpotLineTheme theme, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% AND s.theme = :theme " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.likesCount DESC")
    Page<SpotLine> findByAreaLikeAndThemeAndKeywordAndPopular(
        @Param("area") String area, @Param("theme") SpotLineTheme theme,
        @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% AND s.theme = :theme " +
           "AND (s.title LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<SpotLine> findByAreaLikeAndThemeAndKeywordAndNewest(
        @Param("area") String area, @Param("theme") SpotLineTheme theme,
        @Param("keyword") String keyword, Pageable pageable);

    // ---- FR-01: Spot이 포함된 SpotLine 조회 ----
    @Query("SELECT DISTINCT s FROM SpotLine s " +
           "JOIN FETCH s.spots ss " +
           "JOIN FETCH ss.spot sp " +
           "WHERE sp.id = :spotId AND s.isActive = true " +
           "ORDER BY s.likesCount DESC")
    List<SpotLine> findActiveSpotLinesBySpotId(@Param("spotId") UUID spotId);

    Page<SpotLine> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);

    List<SpotLine> findByCreatorIdInAndIsActiveTrueOrderByCreatedAtDesc(List<String> creatorIds);

    boolean existsBySlug(String slug);

    @Query("SELECT s FROM SpotLine s WHERE s.isActive = true ORDER BY s.updatedAt DESC")
    List<SpotLine> findAllActiveSlugs();

    // ---- Analytics ----

    long countByIsActiveTrue();

    List<SpotLine> findTop10ByIsActiveTrueOrderByViewsCountDesc();

    @Query("SELECT COALESCE(SUM(s.viewsCount), 0) FROM SpotLine s WHERE s.isActive = true")
    long sumViewsCountByIsActiveTrue();

    @Query("SELECT CAST(s.createdAt AS LocalDate) as date, COUNT(s) as cnt " +
           "FROM SpotLine s WHERE s.createdAt >= :since GROUP BY CAST(s.createdAt AS LocalDate)")
    List<Object[]> countDailyCreatedSince(@Param("since") LocalDateTime since);

    long countByCreatorId(String creatorId);
}
