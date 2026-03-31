package com.spotline.api.controller;

import com.spotline.api.dto.response.FollowResponse;
import com.spotline.api.dto.response.FollowStatusResponse;
import com.spotline.api.dto.response.UserProfileResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final AuthUtil authUtil;

    @PostMapping("/{userId}/follow")
    public FollowResponse follow(@PathVariable String userId) {
        return followService.follow(authUtil.requireUserId(), userId);
    }

    @DeleteMapping("/{userId}/follow")
    public FollowResponse unfollow(@PathVariable String userId) {
        return followService.unfollow(authUtil.requireUserId(), userId);
    }

    @GetMapping("/{userId}/follow/status")
    public FollowStatusResponse followStatus(@PathVariable String userId) {
        String currentUserId = authUtil.getCurrentUserId();
        if (currentUserId == null) return new FollowStatusResponse(false);
        return new FollowStatusResponse(followService.isFollowing(currentUserId, userId));
    }

    @GetMapping("/{userId}/followers")
    public Page<UserProfileResponse> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowers(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0));
    }

    @GetMapping("/{userId}/following")
    public Page<UserProfileResponse> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowing(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0));
    }
}
