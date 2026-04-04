package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.UserSpotLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSpotLineRepository extends JpaRepository<UserSpotLine, UUID> {
    Page<UserSpotLine> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<UserSpotLine> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status, Pageable pageable);
}
