package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.SpotLineSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpotLineSaveRepository extends JpaRepository<SpotLineSave, UUID> {
    Optional<SpotLineSave> findByUserIdAndSpotLine(String userId, SpotLine spotLine);
    boolean existsByUserIdAndSpotLine(String userId, SpotLine spotLine);
    Page<SpotLineSave> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long countByUserId(String userId);
}
