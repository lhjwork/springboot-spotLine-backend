package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.UserRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRouteRepository extends JpaRepository<UserRoute, UUID> {
    Page<UserRoute> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<UserRoute> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status, Pageable pageable);
}
