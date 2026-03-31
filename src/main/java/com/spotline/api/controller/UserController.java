package com.spotline.api.controller;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.RouteSaveRepository;
import com.spotline.api.domain.repository.SpotLikeRepository;
import com.spotline.api.domain.repository.SpotSaveRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.response.*;
import com.spotline.api.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final RouteSaveRepository routeSaveRepository;

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다"));
        return UserProfileResponse.from(user, 0, 0);
    }

    @GetMapping("/{userId}/likes/spots")
    public Page<SpotDetailResponse> getLikedSpots(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return spotLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(sl -> SpotDetailResponse.from(sl.getSpot(), null));
    }

    @GetMapping("/{userId}/saves/routes")
    public Page<RoutePreviewResponse> getSavedRoutes(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return routeSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(rs -> RoutePreviewResponse.from(rs.getRoute()));
    }

    @GetMapping("/me/saves")
    public SimplePageResponse<?> getMySaves(
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page) {
        String userId = authUtil.requireUserId();
        Pageable pageable = PageRequest.of(page, 20);

        if ("spot".equals(type)) {
            var saves = spotSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return new SimplePageResponse<>(
                saves.getContent().stream()
                    .map(s -> SpotDetailResponse.from(s.getSpot(), null)).toList(),
                saves.hasNext()
            );
        } else {
            var saves = routeSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return new SimplePageResponse<>(
                saves.getContent().stream()
                    .map(s -> RoutePreviewResponse.from(s.getRoute())).toList(),
                saves.hasNext()
            );
        }
    }
}
