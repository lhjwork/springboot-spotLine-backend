package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.enums.RouteTheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findBySlugAndIsActiveTrue(String slug);

    // ---- Popular (likesCount DESC) ----
    Page<Route> findByIsActiveTrueOrderByLikesCountDesc(Pageable pageable);

    Page<Route> findByAreaAndIsActiveTrueOrderByLikesCountDesc(String area, Pageable pageable);

    Page<Route> findByThemeAndIsActiveTrueOrderByLikesCountDesc(RouteTheme theme, Pageable pageable);

    Page<Route> findByAreaAndThemeAndIsActiveTrueOrderByLikesCountDesc(
            String area, RouteTheme theme, Pageable pageable);

    // ---- Newest (createdAt DESC) ----
    Page<Route> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Route> findByAreaAndIsActiveTrueOrderByCreatedAtDesc(String area, Pageable pageable);

    Page<Route> findByThemeAndIsActiveTrueOrderByCreatedAtDesc(RouteTheme theme, Pageable pageable);

    Page<Route> findByAreaAndThemeAndIsActiveTrueOrderByCreatedAtDesc(
            String area, RouteTheme theme, Pageable pageable);

    // ---- FR-01: Spot이 포함된 Route 조회 ----
    @Query("SELECT DISTINCT r FROM Route r " +
           "JOIN FETCH r.spots rs " +
           "JOIN FETCH rs.spot s " +
           "WHERE s.id = :spotId AND r.isActive = true " +
           "ORDER BY r.likesCount DESC")
    List<Route> findActiveRoutesBySpotId(@Param("spotId") UUID spotId);

    Page<Route> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);

    boolean existsBySlug(String slug);

    @Query("SELECT r FROM Route r WHERE r.isActive = true ORDER BY r.updatedAt DESC")
    List<Route> findAllActiveSlugs();
}
