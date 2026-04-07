package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.BlogLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogLikeRepository extends JpaRepository<BlogLike, UUID> {
    Optional<BlogLike> findByUserIdAndBlog(String userId, Blog blog);
    boolean existsByUserIdAndBlog(String userId, Blog blog);
    Page<BlogLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
