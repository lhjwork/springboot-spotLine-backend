package com.spotline.api.controller;

import com.spotline.api.dto.response.FollowingFeedItemResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feed", description = "피드")
@RestController
@RequestMapping("/api/v2/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final AuthUtil authUtil;

    @Operation(summary = "팔로잉 피드")
    @GetMapping("/following")
    public Page<FollowingFeedItemResponse> getFollowingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authUtil.requireUserId();
        return feedService.getFollowingFeed(userId, PageRequest.of(page, size));
    }
}
