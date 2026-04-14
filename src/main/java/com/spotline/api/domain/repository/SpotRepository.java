package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, UUID> {

    Optional<Spot> findBySlugAndIsActiveTrue(String slug);

    // ---- 기존 (무정렬) ----
    Page<Spot> findByIsActiveTrue(Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrue(SpotCategory category, Pageable pageable);

    // ---- Area 필터: LIKE 매칭 (연남 → 연남, 연남동 모두 매칭) ----
    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.area LIKE %:area% ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndPopular(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.area LIKE %:area% AND s.category = :category ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndCategoryAndPopular(@Param("area") String area, @Param("category") SpotCategory category, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.area LIKE %:area% ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndNewest(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.area LIKE %:area% AND s.category = :category ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndCategoryAndNewest(@Param("area") String area, @Param("category") SpotCategory category, Pageable pageable);

    // ---- Popular (viewsCount DESC) — area 없는 경우 ----
    Page<Spot> findByIsActiveTrueOrderByViewsCountDesc(Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrueOrderByViewsCountDesc(SpotCategory category, Pageable pageable);

    // ---- Newest (createdAt DESC) — area 없는 경우 ----
    Page<Spot> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(SpotCategory category, Pageable pageable);

    // ---- Keyword 검색 (title OR crewNote LIKE) ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByKeywordAndPopular(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByKeywordAndNewest(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndKeywordAndPopular(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndKeywordAndNewest(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByCategoryAndKeywordAndPopular(
        @Param("category") SpotCategory category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByCategoryAndKeywordAndNewest(
        @Param("category") SpotCategory category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndCategoryAndKeywordAndPopular(
        @Param("area") String area, @Param("category") SpotCategory category,
        @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
           "AND s.area LIKE %:area% AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndCategoryAndKeywordAndNewest(
        @Param("area") String area, @Param("category") SpotCategory category,
        @Param("keyword") String keyword, Pageable pageable);

    // ---- Nearby ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
            "AND s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLng AND :maxLng")
    List<Spot> findNearby(
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    Page<Spot> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);

    boolean existsBySlug(String slug);

    Optional<Spot> findByQrIdAndIsActiveTrue(String qrId);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true ORDER BY s.updatedAt DESC")
    List<Spot> findAllActiveSlugs();

    // ---- Analytics ----

    long countByIsActiveTrue();

    List<Spot> findTop10ByIsActiveTrueOrderByViewsCountDesc();

    @Query("SELECT COALESCE(SUM(s.viewsCount), 0) FROM Spot s WHERE s.isActive = true")
    long sumViewsCountByIsActiveTrue();

    @Query("SELECT CAST(s.createdAt AS LocalDate) as date, COUNT(s) as cnt " +
           "FROM Spot s WHERE s.createdAt >= :since GROUP BY CAST(s.createdAt AS LocalDate)")
    List<Object[]> countDailyCreatedSince(@Param("since") LocalDateTime since);

    long countByCreatorId(String creatorId);

    // ---- BI Analytics ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to ORDER BY s.viewsCount DESC")
    List<Spot> findActiveByDateRangeOrderByViews(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to ORDER BY s.likesCount DESC")
    List<Spot> findActiveByDateRangeOrderByLikes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to ORDER BY s.savesCount DESC")
    List<Spot> findActiveByDateRangeOrderBySaves(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to ORDER BY s.commentsCount DESC")
    List<Spot> findActiveByDateRangeOrderByComments(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT s.area, COUNT(s), SUM(s.viewsCount), SUM(s.likesCount) FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to GROUP BY s.area")
    List<Object[]> aggregateByArea(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s.creatorId, s.creatorName, s.creatorType, COUNT(s), SUM(s.viewsCount), SUM(s.likesCount) FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to GROUP BY s.creatorId, s.creatorName, s.creatorType")
    List<Object[]> aggregateByCreator(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(s), COALESCE(SUM(s.viewsCount),0), COALESCE(SUM(s.likesCount),0) FROM Spot s WHERE s.isActive = true AND s.createdAt BETWEEN :from AND :to")
    Object[] aggregateStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
