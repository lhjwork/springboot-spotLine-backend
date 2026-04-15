package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotVisitRepository extends JpaRepository<SpotVisit, UUID> {
    // 기존 (하위 호환)
    Optional<SpotVisit> findByUserIdAndSpot(String userId, Spot spot);
    boolean existsByUserIdAndSpot(String userId, Spot spot);
    Page<SpotVisit> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long countByUserId(String userId);

    // 체크인: 24시간 중복 체크
    boolean existsByUserIdAndSpotAndCreatedAtAfter(String userId, Spot spot, LocalDateTime after);

    // 체크인: 유저의 특정 Spot 체크인 횟수
    long countByUserIdAndSpot(String userId, Spot spot);

    // 체크인: Spot별 체크인 목록
    Page<SpotVisit> findBySpotOrderByCreatedAtDesc(Spot spot, Pageable pageable);

    // 어드민: 기간별 전체 체크인 수
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // 어드민: 기간별 인증 체크인 수
    long countByVerifiedTrueAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // 어드민: 기간별 순 방문자 수
    @Query("SELECT COUNT(DISTINCT sv.userId) FROM SpotVisit sv WHERE sv.createdAt BETWEEN :from AND :to")
    long countDistinctUsersByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 어드민: 기간별 순 Spot 수
    @Query("SELECT COUNT(DISTINCT sv.spot.id) FROM SpotVisit sv WHERE sv.createdAt BETWEEN :from AND :to")
    long countDistinctSpotsByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 어드민: Top Spots by 체크인 수
    @Query("SELECT sv.spot.id, sv.spot.title, sv.spot.area, COUNT(sv) as cnt, COUNT(DISTINCT sv.userId) as visitors, " +
           "SUM(CASE WHEN sv.verified = true THEN 1 ELSE 0 END) as verifiedCnt " +
           "FROM SpotVisit sv WHERE sv.createdAt BETWEEN :from AND :to " +
           "GROUP BY sv.spot.id, sv.spot.title, sv.spot.area ORDER BY cnt DESC")
    List<Object[]> findTopSpotsByCheckins(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    // 어드민: 시간대별 패턴
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at)::int as hour, COUNT(*) as cnt " +
                   "FROM spot_visits WHERE created_at BETWEEN :from AND :to " +
                   "GROUP BY hour ORDER BY hour", nativeQuery = true)
    List<Object[]> findHourlyPattern(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 어드민: 요일별 패턴
    @Query(value = "SELECT EXTRACT(DOW FROM created_at)::int as dow, COUNT(*) as cnt " +
                   "FROM spot_visits WHERE created_at BETWEEN :from AND :to " +
                   "GROUP BY dow ORDER BY dow", nativeQuery = true)
    List<Object[]> findDailyPattern(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
