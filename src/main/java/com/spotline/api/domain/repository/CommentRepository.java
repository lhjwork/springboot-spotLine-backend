package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Comment;
import com.spotline.api.domain.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtDesc(
            CommentTargetType targetType, UUID targetId, Pageable pageable);

    long countByTargetTypeAndTargetIdAndIsDeletedFalse(
            CommentTargetType targetType, UUID targetId);
}
