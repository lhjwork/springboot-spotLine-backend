package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Comment;
import com.spotline.api.domain.enums.CommentTargetType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private CommentTargetType targetType;
    private UUID targetId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String content;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies;

    public static CommentResponse from(Comment comment) {
        List<CommentResponse> replyResponses = Collections.emptyList();
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            replyResponses = comment.getReplies().stream()
                    .map(CommentResponse::from)
                    .toList();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .userId(comment.getUserId())
                .userName(comment.getIsDeleted() ? "" : comment.getUserName())
                .userAvatarUrl(comment.getIsDeleted() ? null : comment.getUserAvatarUrl())
                .content(comment.getIsDeleted() ? "삭제된 댓글입니다" : comment.getContent())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replyResponses)
                .build();
    }
}
