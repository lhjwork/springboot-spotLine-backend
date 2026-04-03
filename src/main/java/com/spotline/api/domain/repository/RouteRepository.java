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

    Page<Route> findByThemeAndIsActiveTrueOrderByLikesCountDesc(RouteTheme theme, Pageable pageable);

    // ---- Area LIKE 매칭 (연남 → 연남, 연남동 모두 매칭) ----
    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.area LIKE %:area% ORDER BY r.likesCount DESC")
    Page<Route> findByAreaLikeAndPopular(@Param("area") String area, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.area LIKE %:area% AND r.theme = :theme ORDER BY r.likesCount DESC")
    Page<Route> findByAreaLikeAndThemeAndPopular(@Param("area") String area, @Param("theme") RouteTheme theme, Pageable pageable);

    // ---- Newest (createdAt DESC) ----
    Page<Route> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Route> findByThemeAndIsActiveTrueOrderByCreatedAtDesc(RouteTheme theme, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.area LIKE %:area% ORDER BY r.createdAt DESC")
    Page<Route> findByAreaLikeAndNewest(@Param("area") String area, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true AND r.area LIKE %:area% AND r.theme = :theme ORDER BY r.createdAt DESC")
    Page<Route> findByAreaLikeAndThemeAndNewest(@Param("area") String area, @Param("theme") RouteTheme theme, Pageable pageable);

    // ---- Keyword 검색 (title OR description LIKE) ----

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.likesCount DESC")
    Page<Route> findByKeywordAndPopular(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<Route> findByKeywordAndNewest(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.area LIKE %:area% " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.likesCount DESC")
    Page<Route> findByAreaLikeAndKeywordAndPopular(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.area LIKE %:area% " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<Route> findByAreaLikeAndKeywordAndNewest(
        @Param("area") String area, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.theme = :theme " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.likesCount DESC")
    Page<Route> findByThemeAndKeywordAndPopular(
        @Param("theme") RouteTheme theme, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.theme = :theme " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<Route> findByThemeAndKeywordAndNewest(
        @Param("theme") RouteTheme theme, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.area LIKE %:area% AND r.theme = :theme " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.likesCount DESC")
    Page<Route> findByAreaLikeAndThemeAndKeywordAndPopular(
        @Param("area") String area, @Param("theme") RouteTheme theme,
        @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.isActive = true " +
           "AND r.area LIKE %:area% AND r.theme = :theme " +
           "AND (r.title LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<Route> findByAreaLikeAndThemeAndKeywordAndNewest(
        @Param("area") String area, @Param("theme") RouteTheme theme,
        @Param("keyword") String keyword, Pageable pageable);

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
