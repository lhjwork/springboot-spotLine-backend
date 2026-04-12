package com.spotline.api.service;

import com.spotline.api.domain.entity.Comment;
import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.enums.CommentTargetType;
import com.spotline.api.domain.enums.NotificationType;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.CommentRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.dto.request.CreateCommentRequest;
import com.spotline.api.dto.request.UpdateCommentRequest;
import com.spotline.api.dto.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final BlogRepository blogRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(CommentTargetType targetType, UUID targetId, int page, int size) {
        int clampedSize = Math.min(size, 50);
        Page<Comment> comments = commentRepository
                .findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtDesc(
                        targetType, targetId, PageRequest.of(page, clampedSize));
        return comments.map(CommentResponse::from);
    }

    public CommentResponse createComment(String userId, String userName, String userAvatarUrl,
                                          CreateCommentRequest request) {
        validateTargetExists(request.getTargetType(), request.getTargetId());

        Comment.CommentBuilder builder = Comment.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .userId(userId)
                .userName(userName)
                .userAvatarUrl(userAvatarUrl)
                .content(request.getContent());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다"));

            if (parent.getParent() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다");
            }

            if (!parent.getTargetType().equals(request.getTargetType())
                    || !parent.getTargetId().equals(request.getTargetId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "부모 댓글과 대상이 일치하지 않습니다");
            }

            builder.parent(parent);
        }

        Comment saved = commentRepository.save(builder.build());
        updateCommentsCount(request.getTargetType(), request.getTargetId(), 1);

        try {
            String ownerId = getTargetOwnerId(request.getTargetType(), request.getTargetId());
            String slug = getTargetSlug(request.getTargetType(), request.getTargetId());
            if (ownerId != null) {
                notificationService.create(userId, ownerId, NotificationType.COMMENT,
                    request.getTargetType().name(), request.getTargetId().toString(), slug);
            }
        } catch (Exception ignored) {}

        return CommentResponse.from(saved);
    }

    public CommentResponse updateComment(String userId, UUID commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));

        if (comment.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제된 댓글은 수정할 수 없습니다");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 댓글만 수정할 수 있습니다");
        }

        comment.setContent(request.getContent());
        return CommentResponse.from(commentRepository.save(comment));
    }

    public void deleteComment(String userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));

        if (comment.getIsDeleted()) {
            return;
        }

        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 댓글만 삭제할 수 있습니다");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        updateCommentsCount(comment.getTargetType(), comment.getTargetId(), -1);
    }

    private void validateTargetExists(CommentTargetType targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case SPOT -> spotRepository.existsById(targetId);
            case SPOTLINE -> spotLineRepository.existsById(targetId);
            case BLOG -> blogRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다");
        }
    }

    private String getTargetOwnerId(CommentTargetType type, UUID targetId) {
        return switch (type) {
            case SPOT -> spotRepository.findById(targetId).map(Spot::getCreatorId).orElse(null);
            case SPOTLINE -> spotLineRepository.findById(targetId).map(SpotLine::getCreatorId).orElse(null);
            case BLOG -> blogRepository.findById(targetId).map(Blog::getUserId).orElse(null);
        };
    }

    private String getTargetSlug(CommentTargetType type, UUID targetId) {
        return switch (type) {
            case SPOT -> spotRepository.findById(targetId).map(Spot::getSlug).orElse(null);
            case SPOTLINE -> spotLineRepository.findById(targetId).map(SpotLine::getSlug).orElse(null);
            case BLOG -> blogRepository.findById(targetId).map(Blog::getSlug).orElse(null);
        };
    }

    private void updateCommentsCount(CommentTargetType targetType, UUID targetId, int delta) {
        switch (targetType) {
            case SPOT -> spotRepository.findById(targetId).ifPresent(spot -> {
                spot.setCommentsCount(Math.max(0, spot.getCommentsCount() + delta));
                spotRepository.save(spot);
            });
            case SPOTLINE -> spotLineRepository.findById(targetId).ifPresent(spotLine -> {
                spotLine.setCommentsCount(Math.max(0, spotLine.getCommentsCount() + delta));
                spotLineRepository.save(spotLine);
            });
            case BLOG -> blogRepository.findById(targetId).ifPresent(blog -> {
                blog.setCommentsCount(Math.max(0, blog.getCommentsCount() + delta));
                blogRepository.save(blog);
            });
        }
    }
}
