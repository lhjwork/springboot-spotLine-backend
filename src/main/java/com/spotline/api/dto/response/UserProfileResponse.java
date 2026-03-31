package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String id;
    private String nickname;
    private String avatar;
    private String bio;
    private String joinedAt;
    private String instagramId;
    private UserStatsResponse stats;

    @Data
    @Builder
    public static class UserStatsResponse {
        private int visited;
        private int liked;
        private int recommended;
        private int spotlines;
        private int followers;
        private int following;
    }

    public static UserProfileResponse from(User user, int likedCount, int savedCount) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .bio(user.getBio())
            .joinedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
            .instagramId(user.getInstagramId())
            .stats(UserStatsResponse.builder()
                .visited(0)
                .liked(likedCount)
                .recommended(0)
                .spotlines(savedCount)
                .followers(user.getFollowersCount())
                .following(user.getFollowingCount())
                .build())
            .build();
    }
}
