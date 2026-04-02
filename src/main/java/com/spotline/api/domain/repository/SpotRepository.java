package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
