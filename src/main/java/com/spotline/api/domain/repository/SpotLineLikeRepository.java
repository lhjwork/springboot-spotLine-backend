package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.SpotLineLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpotLineLikeRepository extends JpaRepository<SpotLineLike, UUID> {
    Optional<SpotLineLike> findByUserIdAndSpotLine(String userId, SpotLine spotLine);
    boolean existsByUserIdAndSpotLine(String userId, SpotLine spotLine);
    Page<SpotLineLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
