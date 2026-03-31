package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    Optional<UserFollow> findByFollowerIdAndFollowingId(String followerId, String followingId);
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
    Page<UserFollow> findByFollowingIdOrderByCreatedAtDesc(String followingId, Pageable pageable);
    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(String followerId, Pageable pageable);
}
