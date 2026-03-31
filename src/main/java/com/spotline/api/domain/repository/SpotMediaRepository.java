package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.SpotMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpotMediaRepository extends JpaRepository<SpotMedia, UUID> {

    List<SpotMedia> findBySpotIdOrderByDisplayOrderAsc(UUID spotId);

    void deleteBySpotId(UUID spotId);
}
