package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.RouteSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RouteSaveRepository extends JpaRepository<RouteSave, UUID> {
    Optional<RouteSave> findByUserIdAndRoute(String userId, Route route);
    boolean existsByUserIdAndRoute(String userId, Route route);
    Page<RouteSave> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long countByUserId(String userId);
}
