package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findBySlugAndIsActiveTrue(String slug);

    /** 공개 블로그 목록 (최신순) */
    Page<Blog> findByStatusAndIsActiveTrueOrderByPublishedAtDesc(BlogStatus status, Pageable pageable);

    /** 공개 블로그 목록 (지역 필터) */
    @Query("SELECT b FROM Blog b JOIN b.spotLine sl WHERE b.status = 'PUBLISHED' AND b.isActive = true " +
           "AND sl.area LIKE %:area% ORDER BY b.publishedAt DESC")
    Page<Blog> findPublishedByArea(@Param("area") String area, Pageable pageable);

    /** 내 블로그 (Draft + Published) */
    Page<Blog> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId, Pageable pageable);

    /** 내 블로그 상태별 */
    Page<Blog> findByUserIdAndStatusAndIsActiveTrueOrderByUpdatedAtDesc(String userId, BlogStatus status, Pageable pageable);

    /** slug 중복 체크 */
    boolean existsBySlug(String slug);

    /** 전체 공개 slug 목록 (sitemap/SSR) */
    @Query("SELECT b.slug FROM Blog b WHERE b.status = 'PUBLISHED' AND b.isActive = true")
    List<String> findAllPublishedSlugs();

    /** 팔로잉 피드 — 여러 유저의 공개 블로그 */
    List<Blog> findByUserIdInAndStatusAndIsActiveTrueOrderByPublishedAtDesc(List<String> userIds, BlogStatus status);

    /** SpotLine별 블로그 수 */
    long countBySpotLineIdAndIsActiveTrue(UUID spotLineId);
}
