package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.RouteLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RouteLikeRepository extends JpaRepository<RouteLike, UUID> {
    Optional<RouteLike> findByUserIdAndRoute(String userId, Route route);
    boolean existsByUserIdAndRoute(String userId, Route route);
    Page<RouteLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
