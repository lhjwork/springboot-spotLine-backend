package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotStatus;
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

    // ---- Public queries (APPROVED or legacy NULL status) ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) ORDER BY s.viewsCount DESC")
    Page<Spot> findApprovedOrderByViewsCountDesc(Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.category = :category ORDER BY s.viewsCount DESC")
    Page<Spot> findApprovedByCategoryOrderByViewsCountDesc(@Param("category") SpotCategory category, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) ORDER BY s.createdAt DESC")
    Page<Spot> findApprovedOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.category = :category ORDER BY s.createdAt DESC")
    Page<Spot> findApprovedByCategoryOrderByCreatedAtDesc(@Param("category") SpotCategory category, Pageable pageable);

    // ---- Area filter: LIKE matching ----
    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.area LIKE %:area% ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndPopular(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.area LIKE %:area% AND s.category = :category ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndCategoryAndPopular(@Param("area") String area, @Param("category") SpotCategory category, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.area LIKE %:area% ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndNewest(@Param("area") String area, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) AND s.area LIKE %:area% AND s.category = :category ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndCategoryAndNewest(@Param("area") String area, @Param("category") SpotCategory category, Pageable pageable);

    // ---- Keyword search ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByKeywordAndPopular(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByKeywordAndNewest(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndKeywordAndPopular(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.area LIKE %:area% " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndKeywordAndNewest(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByCategoryAndKeywordAndPopular(
        @Param("category") SpotCategory category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByCategoryAndKeywordAndNewest(
        @Param("category") SpotCategory category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.area LIKE %:area% AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.viewsCount DESC")
    Page<Spot> findByAreaLikeAndCategoryAndKeywordAndPopular(
        @Param("area") String area, @Param("category") SpotCategory category,
        @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
           "AND s.area LIKE %:area% AND s.category = :category " +
           "AND (s.title LIKE %:keyword% OR s.crewNote LIKE %:keyword%) " +
           "ORDER BY s.createdAt DESC")
    Page<Spot> findByAreaLikeAndCategoryAndKeywordAndNewest(
        @Param("area") String area, @Param("category") SpotCategory category,
        @Param("keyword") String keyword, Pageable pageable);

    // ---- Nearby (public — APPROVED filter) ----

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) " +
            "AND s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLng AND :maxLng")
    List<Spot> findNearby(
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    // ---- My Spots (all statuses for owner) ----
    Page<Spot> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.creatorId = :creatorId AND s.isActive = true AND s.status = :status ORDER BY s.createdAt DESC")
    Page<Spot> findByCreatorIdAndStatusOrderByCreatedAtDesc(@Param("creatorId") String creatorId, @Param("status") SpotStatus status, Pageable pageable);

    // ---- Admin: Pending review ----
    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND s.status = :status ORDER BY s.createdAt ASC")
    Page<Spot> findByStatusOrderByCreatedAtAsc(@Param("status") SpotStatus status, Pageable pageable);

    long countByStatusAndIsActiveTrue(SpotStatus status);

    boolean existsBySlug(String slug);

    Optional<Spot> findByQrIdAndIsActiveTrue(String qrId);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true AND (s.status = 'APPROVED' OR s.status IS NULL) ORDER BY s.updatedAt DESC")
    List<Spot> findAllActiveSlugs();

    // ---- Unfiltered (admin/internal) ----
    Page<Spot> findByIsActiveTrue(Pageable pageable);

    // ---- Analytics ----

    long countByIsActiveTrue();

    List<Spot> findTop10ByIsActiveTrueOrderByViewsCountDesc();

    @Query("SELECT COALESCE(SUM(s.viewsCount), 0) FROM Spot s WHERE s.isActive = true")
    long sumViewsCountByIsActiveTrue();

    @Query("SELECT CAST(s.createdAt AS LocalDate) as date, COUNT(s) as cnt " +
           "FROM Spot s WHERE s.createdAt >= :since GROUP BY CAST(s.createdAt AS LocalDate)")
    List<Object[]> countDailyCreatedSince(@Param("since") LocalDateTime since);

    long countByCreatorId(String creatorId);

    long countByCreatorIdAndIsActiveTrue(String creatorId);

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
