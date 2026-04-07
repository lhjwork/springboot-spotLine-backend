package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "어드민 유저 목록 응답")
@Data
@Builder
public class UserAdminResponse {

    private String id;
    private String email;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private Boolean suspended;
    private Integer followersCount;
    private Integer followingCount;
    private long spotsCount;
    private long spotLinesCount;
    private long blogsCount;
    private LocalDateTime createdAt;
    private LocalDateTime suspendedAt;

    public static UserAdminResponse from(User user, long spotsCount, long spotLinesCount, long blogsCount) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole())
                .suspended(user.getSuspended() != null && user.getSuspended())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .spotsCount(spotsCount)
                .spotLinesCount(spotLinesCount)
                .blogsCount(blogsCount)
                .createdAt(user.getCreatedAt())
                .suspendedAt(user.getSuspendedAt())
                .build();
    }
}
