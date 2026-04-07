package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.BlogSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogSaveRepository extends JpaRepository<BlogSave, UUID> {
    Optional<BlogSave> findByUserIdAndBlog(String userId, Blog blog);
    boolean existsByUserIdAndBlog(String userId, Blog blog);
    Page<BlogSave> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
