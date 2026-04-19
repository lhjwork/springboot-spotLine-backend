package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {
}
