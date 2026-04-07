package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.BlogBlockMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlogBlockMediaRepository extends JpaRepository<BlogBlockMedia, UUID> {
}
