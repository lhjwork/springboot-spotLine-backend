package com.spotline.api.service;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.SpotLineSaveRepository;
import com.spotline.api.domain.repository.SpotLikeRepository;
import com.spotline.api.dto.request.UpdateProfileRequest;
import com.spotline.api.dto.response.AvatarUploadResponse;
import com.spotline.api.dto.response.UserProfileResponse;
import com.spotline.api.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserSyncService userSyncService;
    private final S3Service s3Service;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotLineSaveRepository spotLineSaveRepository;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );

    @Transactional
    public UserProfileResponse updateProfile(String userId, String email, UpdateProfileRequest request) {
        User user = userSyncService.getOrCreateUser(userId, email);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isEmpty() ? null : request.getBio());
        }
        if (request.getInstagramId() != null) {
            String instaId = request.getInstagramId().startsWith("@")
                ? request.getInstagramId().substring(1)
                : request.getInstagramId();
            user.setInstagramId(instaId.isEmpty() ? null : instaId);
        }

        return buildProfileResponse(user);
    }

    @Transactional
    public AvatarUploadResponse generateAvatarUploadUrl(String userId, String email,
                                                         String filename, String contentType) {
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 가능합니다");
        }

        User user = userSyncService.getOrCreateUser(userId, email);

        if (user.getAvatar() != null) {
            try {
                String oldKey = extractS3Key(user.getAvatar());
                s3Service.deleteObject(oldKey);
            } catch (Exception ignored) {
            }
        }

        String ext = extractExtension(filename);
        String avatarKey = "avatars/" + userId + "/" + System.currentTimeMillis() + "." + ext;
        String presignedUrl = s3Service.generatePresignedUploadUrl(avatarKey, contentType);
        String avatarUrl = s3Service.getPublicUrl(avatarKey);

        user.setAvatar(avatarUrl);

        return AvatarUploadResponse.builder()
            .presignedUrl(presignedUrl)
            .avatarKey(avatarKey)
            .avatarUrl(avatarUrl)
            .build();
    }

    @Transactional
    public UserProfileResponse deleteAvatar(String userId, String email) {
        User user = userSyncService.getOrCreateUser(userId, email);

        if (user.getAvatar() != null) {
            try {
                String oldKey = extractS3Key(user.getAvatar());
                s3Service.deleteObject(oldKey);
            } catch (Exception ignored) {
            }
            user.setAvatar(null);
        }

        return buildProfileResponse(user);
    }

    private UserProfileResponse buildProfileResponse(User user) {
        int likedCount = (int) spotLikeRepository.countByUserId(user.getId());
        int savedCount = (int) spotLineSaveRepository.countByUserId(user.getId());
        return UserProfileResponse.from(user, likedCount, savedCount);
    }

    private String extractS3Key(String url) {
        int idx = url.indexOf(".amazonaws.com/");
        return idx >= 0 ? url.substring(idx + 15) : url;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1).toLowerCase() : "jpg";
    }
}
