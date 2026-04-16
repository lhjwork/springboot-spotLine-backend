package com.spotline.api.controller;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotLineSaveRepository;
import com.spotline.api.domain.repository.SpotLikeRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.domain.repository.SpotSaveRepository;
import com.spotline.api.domain.repository.SpotVisitRepository;
import com.spotline.api.domain.repository.BlogRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.domain.enums.BlogStatus;
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
    private final SpotVisitRepository spotVisitRepository;
    private final BlogRepository blogRepository;
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
        int likedCount = (int) spotLikeRepository.countByUserId(userId);
        int savedCount = (int) spotLineSaveRepository.countByUserId(userId);
        int visitedCount = (int) spotVisitRepository.countByUserId(userId);
        int spotsCount = (int) spotRepository.countByCreatorIdAndIsActiveTrue(userId);
        int spotLinesCount = (int) spotLineRepository.countByCreatorIdAndIsActiveTrue(userId);
        int blogsCount = (int) blogRepository.countByUserId(userId);
        return UserProfileResponse.from(user, likedCount, savedCount, visitedCount,
            spotsCount, spotLinesCount, blogsCount);
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

    @Operation(summary = "사용자 방문 스팟")
    @GetMapping("/{userId}/visited-spots")
    public Page<SpotDetailResponse> getVisitedSpots(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return spotVisitRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(sv -> SpotDetailResponse.from(sv.getSpot(), null));
    }

    @Operation(summary = "사용자 생성 SpotLine 목록 (공개)")
    @GetMapping("/{userId}/spotlines-created")
    public SimplePageResponse<SpotLinePreviewResponse> getUserSpotLines(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SpotLine> spotLines = spotLineRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size));
        return new SimplePageResponse<>(
            spotLines.getContent().stream().map(SpotLinePreviewResponse::from).toList(),
            spotLines.hasNext()
        );
    }

    @Operation(summary = "사용자 생성 Spot 목록 (공개)")
    @GetMapping("/{userId}/spots")
    public SimplePageResponse<SpotDetailResponse> getUserSpots(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Spot> spots = spotRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size));
        return new SimplePageResponse<>(
            spots.getContent().stream().map(s -> SpotDetailResponse.from(s, null)).toList(),
            spots.hasNext()
        );
    }

    @Operation(summary = "사용자 작성 Blog 목록 (공개)")
    @GetMapping("/{userId}/blogs")
    public SimplePageResponse<BlogDetailResponse> getUserBlogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Blog> blogs = blogRepository.findByUserIdAndStatusAndIsActiveTrueOrderByUpdatedAtDesc(
            userId, BlogStatus.PUBLISHED, PageRequest.of(page, size));
        return new SimplePageResponse<>(
            blogs.getContent().stream().map(BlogDetailResponse::from).toList(),
            blogs.hasNext()
        );
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
