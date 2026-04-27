package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Collection;
import com.spotline.api.domain.enums.SpotLineTheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    Optional<Collection> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    List<Collection> findByIsFeaturedTrueAndIsPublishedTrueAndIsActiveTrueOrderByDisplayOrderAsc();

    Page<Collection> findByIsPublishedTrueAndIsActiveTrueOrderByDisplayOrderAsc(Pageable pageable);

    @Query("SELECT c FROM Collection c WHERE c.isPublished = true AND c.isActive = true " +
            "AND (:area IS NULL OR c.area LIKE %:area%) " +
            "AND (:theme IS NULL OR c.theme = :theme) " +
            "ORDER BY c.displayOrder ASC")
    Page<Collection> findByFilters(@Param("area") String area,
                                   @Param("theme") SpotLineTheme theme,
                                   Pageable pageable);

    Page<Collection> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM Collection c WHERE c.isActive = true " +
            "AND (:keyword IS NULL OR c.title LIKE %:keyword%) " +
            "ORDER BY c.createdAt DESC")
    Page<Collection> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
