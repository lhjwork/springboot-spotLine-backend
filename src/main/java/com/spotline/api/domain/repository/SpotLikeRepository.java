package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpotLikeRepository extends JpaRepository<SpotLike, UUID> {
    Optional<SpotLike> findByUserIdAndSpot(String userId, Spot spot);
    boolean existsByUserIdAndSpot(String userId, Spot spot);
    Page<SpotLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
