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

    Page<Spot> findByIsActiveTrue(Pageable pageable);

    Page<Spot> findByAreaAndIsActiveTrue(String area, Pageable pageable);

    Page<Spot> findByCategoryAndIsActiveTrue(SpotCategory category, Pageable pageable);

    Page<Spot> findByAreaAndCategoryAndIsActiveTrue(String area, SpotCategory category, Pageable pageable);

    @Query("SELECT s FROM Spot s WHERE s.isActive = true " +
            "AND s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLng AND :maxLng")
    List<Spot> findNearby(
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    boolean existsBySlug(String slug);

    Optional<Spot> findByQrIdAndIsActiveTrue(String qrId);
}
