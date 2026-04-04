package com.spotline.api.controller;

import com.spotline.api.dto.response.SocialStatusResponse;
import com.spotline.api.dto.response.SocialToggleResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Social", description = "좋아요/저장 토글")
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final AuthUtil authUtil;

    @Operation(summary = "스팟 좋아요 토글")
    @PostMapping("/spots/{id}/like")
    public SocialToggleResponse toggleSpotLike(@PathVariable UUID id) {
        return socialService.toggleSpotLike(authUtil.requireUserId(), id);
    }

    @Operation(summary = "스팟 저장 토글")
    @PostMapping("/spots/{id}/save")
    public SocialToggleResponse toggleSpotSave(@PathVariable UUID id) {
        return socialService.toggleSpotSave(authUtil.requireUserId(), id);
    }

    @Operation(summary = "SpotLine 좋아요 토글")
    @PostMapping("/spotlines/{id}/like")
    public SocialToggleResponse toggleSpotLineLike(@PathVariable UUID id) {
        return socialService.toggleSpotLineLike(authUtil.requireUserId(), id);
    }

    @Operation(summary = "SpotLine 저장 토글")
    @PostMapping("/spotlines/{id}/save")
    public SocialToggleResponse toggleSpotLineSave(@PathVariable UUID id) {
        return socialService.toggleSpotLineSave(authUtil.requireUserId(), id);
    }

    @Operation(summary = "스팟 소셜 상태 조회")
    @GetMapping("/spots/{id}/social")
    public SocialStatusResponse getSpotSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getSpotSocialStatus(userId, id);
    }

    @Operation(summary = "SpotLine 소셜 상태 조회")
    @GetMapping("/spotlines/{id}/social")
    public SocialStatusResponse getSpotLineSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getSpotLineSocialStatus(userId, id);
    }
}
