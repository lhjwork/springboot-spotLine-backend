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

    Page<Spot> findByAreaAndIsActiveTrue(String area, Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrue(SpotCategory category, Pageable pageable);

    Page<Spot> findByAreaAndCategoryAndIsActiveTrue(String area, SpotCategory category, Pageable pageable);

    // ---- Popular (viewsCount DESC) ----
    Page<Spot> findByIsActiveTrueOrderByViewsCountDesc(Pageable pageable);

    Page<Spot> findByAreaAndIsActiveTrueOrderByViewsCountDesc(String area, Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrueOrderByViewsCountDesc(SpotCategory category, Pageable pageable);

    Page<Spot> findByAreaAndCategoryAndIsActiveTrueOrderByViewsCountDesc(
            String area, SpotCategory category, Pageable pageable);

    // ---- Newest (createdAt DESC) ----
    Page<Spot> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Spot> findByAreaAndIsActiveTrueOrderByCreatedAtDesc(String area, Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(SpotCategory category, Pageable pageable);

    Page<Spot> findByAreaAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(
            String area, SpotCategory category, Pageable pageable);

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
