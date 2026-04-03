package com.spotline.api.controller;

import com.spotline.api.domain.enums.CommentTargetType;
import com.spotline.api.dto.request.CreateCommentRequest;
import com.spotline.api.dto.request.UpdateCommentRequest;
import com.spotline.api.dto.response.CommentResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final AuthUtil authUtil;

    @GetMapping
    public Page<CommentResponse> getComments(
            @RequestParam CommentTargetType targetType,
            @RequestParam UUID targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentService.getComments(targetType, targetId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@Valid @RequestBody CreateCommentRequest request) {
        String userId = authUtil.requireUserId();
        String email = authUtil.getCurrentEmail();
        String userName = email != null ? email.split("@")[0] : "사용자";
        return commentService.createComment(userId, userName, null, request);
    }

    @PutMapping("/{id}")
    public CommentResponse updateComment(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(authUtil.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable UUID id) {
        commentService.deleteComment(authUtil.requireUserId(), id);
    }
}
