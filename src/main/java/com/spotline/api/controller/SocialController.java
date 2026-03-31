package com.spotline.api.controller;

import com.spotline.api.dto.response.SocialStatusResponse;
import com.spotline.api.dto.response.SocialToggleResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final AuthUtil authUtil;

    @PostMapping("/spots/{id}/like")
    public SocialToggleResponse toggleSpotLike(@PathVariable UUID id) {
        return socialService.toggleSpotLike(authUtil.requireUserId(), id);
    }

    @PostMapping("/spots/{id}/save")
    public SocialToggleResponse toggleSpotSave(@PathVariable UUID id) {
        return socialService.toggleSpotSave(authUtil.requireUserId(), id);
    }

    @PostMapping("/routes/{id}/like")
    public SocialToggleResponse toggleRouteLike(@PathVariable UUID id) {
        return socialService.toggleRouteLike(authUtil.requireUserId(), id);
    }

    @PostMapping("/routes/{id}/save")
    public SocialToggleResponse toggleRouteSave(@PathVariable UUID id) {
        return socialService.toggleRouteSave(authUtil.requireUserId(), id);
    }

    @GetMapping("/spots/{id}/social")
    public SocialStatusResponse getSpotSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getSpotSocialStatus(userId, id);
    }

    @GetMapping("/routes/{id}/social")
    public SocialStatusResponse getRouteSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getRouteSocialStatus(userId, id);
    }
}
