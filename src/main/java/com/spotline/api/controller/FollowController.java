package com.spotline.api.controller;

import com.spotline.api.dto.response.FollowResponse;
import com.spotline.api.dto.response.FollowStatusResponse;
import com.spotline.api.dto.response.UserProfileResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Follow", description = "사용자 팔로우/팔로워")
@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final AuthUtil authUtil;

    @Operation(summary = "팔로우")
    @PostMapping("/{userId}/follow")
    public FollowResponse follow(@PathVariable String userId) {
        return followService.follow(authUtil.requireUserId(), userId);
    }

    @Operation(summary = "언팔로우")
    @DeleteMapping("/{userId}/follow")
    public FollowResponse unfollow(@PathVariable String userId) {
        return followService.unfollow(authUtil.requireUserId(), userId);
    }

    @Operation(summary = "팔로우 상태 확인")
    @GetMapping("/{userId}/follow/status")
    public FollowStatusResponse followStatus(@PathVariable String userId) {
        String currentUserId = authUtil.getCurrentUserId();
        if (currentUserId == null) return new FollowStatusResponse(false);
        return new FollowStatusResponse(followService.isFollowing(currentUserId, userId));
    }

    @Operation(summary = "팔로워 목록")
    @GetMapping("/{userId}/followers")
    public Page<UserProfileResponse> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowers(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0, 0));
    }

    @Operation(summary = "팔로잉 목록")
    @GetMapping("/{userId}/following")
    public Page<UserProfileResponse> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowing(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0, 0));
    }
}
