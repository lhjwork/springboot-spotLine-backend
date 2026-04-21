package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Share;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {

    @Query("SELECT s.channel, COUNT(s) FROM Share s " +
           "WHERE s.createdAt >= :since " +
           "AND (:targetType IS NULL OR s.targetType = :targetType) " +
           "GROUP BY s.channel")
    List<Object[]> countByChannelSince(@Param("since") LocalDateTime since,
                                       @Param("targetType") String targetType);

    @Query("SELECT CAST(s.createdAt AS date), COUNT(s) FROM Share s " +
           "WHERE s.createdAt >= :since " +
           "GROUP BY CAST(s.createdAt AS date) " +
           "ORDER BY CAST(s.createdAt AS date)")
    List<Object[]> dailyTrendSince(@Param("since") LocalDateTime since);

    @Query("SELECT s.targetId, s.targetType, COUNT(s) as cnt FROM Share s " +
           "WHERE s.createdAt >= :since AND s.targetType = :targetType " +
           "GROUP BY s.targetId, s.targetType " +
           "ORDER BY cnt DESC")
    List<Object[]> topSharedContent(@Param("since") LocalDateTime since,
                                    @Param("targetType") String targetType,
                                    Pageable pageable);
}
