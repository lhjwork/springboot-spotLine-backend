package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.BlogBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlogBlockRepository extends JpaRepository<BlogBlock, UUID> {

    List<BlogBlock> findByBlogIdOrderByBlockOrderAsc(UUID blogId);

    void deleteByBlogId(UUID blogId);
}
