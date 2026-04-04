package com.spotline.api.controller;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotLineSaveRepository;
import com.spotline.api.domain.repository.SpotLikeRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.SpotSaveRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.request.AvatarUploadRequest;
import com.spotline.api.dto.request.UpdateProfileRequest;
import com.spotline.api.dto.response.*;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "User", description = "사용자 프로필 + 내 컨텐츠")
@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final SpotLineSaveRepository spotLineSaveRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final UserProfileService userProfileService;

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me/profile")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(
            authUtil.requireUserId(), authUtil.getCurrentEmail(), request);
    }

    @Operation(summary = "아바타 업로드")
    @PostMapping("/me/avatar")
    public AvatarUploadResponse uploadAvatar(@Valid @RequestBody AvatarUploadRequest request) {
        return userProfileService.generateAvatarUploadUrl(
            authUtil.requireUserId(), authUtil.getCurrentEmail(),
            request.getFilename(), request.getContentType());
    }

    @Operation(summary = "아바타 삭제")
    @DeleteMapping("/me/avatar")
    public UserProfileResponse deleteAvatar() {
        return userProfileService.deleteAvatar(
            authUtil.requireUserId(), authUtil.getCurrentEmail());
    }

    @Operation(summary = "내가 생성한 스팟")
    @GetMapping("/me/spots")
    public SimplePageResponse<SpotDetailResponse> getMySpots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authUtil.requireUserId();
        Page<Spot> spots = spotRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size));
        return new SimplePageResponse<>(
            spots.getContent().stream().map(s -> SpotDetailResponse.from(s, null)).toList(),
            spots.hasNext()
        );
    }

    @Operation(summary = "내가 생성한 SpotLine")
    @GetMapping("/me/spotlines-created")
    public SimplePageResponse<SpotLinePreviewResponse> getMyCreatedSpotLines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authUtil.requireUserId();
        Page<SpotLine> spotLines = spotLineRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size));
        return new SimplePageResponse<>(
            spotLines.getContent().stream().map(SpotLinePreviewResponse::from).toList(),
            spotLines.hasNext()
        );
    }

    @Operation(summary = "사용자 프로필 조회")
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다"));
        return UserProfileResponse.from(user, 0, 0);
    }

    @Operation(summary = "사용자 좋아요 스팟")
    @GetMapping("/{userId}/likes/spots")
    public Page<SpotDetailResponse> getLikedSpots(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return spotLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(sl -> SpotDetailResponse.from(sl.getSpot(), null));
    }

    @Operation(summary = "사용자 저장 SpotLine")
    @GetMapping("/{userId}/saves/spotlines")
    public Page<SpotLinePreviewResponse> getSavedSpotLines(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return spotLineSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(rs -> SpotLinePreviewResponse.from(rs.getSpotLine()));
    }

    @Operation(summary = "내 저장 목록")
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
            var saves = spotLineSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return new SimplePageResponse<>(
                saves.getContent().stream()
                    .map(s -> SpotLinePreviewResponse.from(s.getSpotLine())).toList(),
                saves.hasNext()
            );
        }
    }
}
