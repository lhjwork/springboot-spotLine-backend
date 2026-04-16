package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "사용자 프로필 응답")
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
        private int spotsCount;
        private int spotLinesCount;
        private int blogsCount;
        private int followers;
        private int following;
    }

    public static UserProfileResponse from(User user, int likedCount, int savedCount, int visitedCount) {
        return from(user, likedCount, savedCount, visitedCount, 0, 0, 0);
    }

    public static UserProfileResponse from(User user, int likedCount, int savedCount, int visitedCount,
                                             int spotsCount, int spotLinesCount, int blogsCount) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .bio(user.getBio())
            .joinedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
            .instagramId(user.getInstagramId())
            .stats(UserStatsResponse.builder()
                .visited(visitedCount)
                .liked(likedCount)
                .recommended(0)
                .spotlines(savedCount)
                .spotsCount(spotsCount)
                .spotLinesCount(spotLinesCount)
                .blogsCount(blogsCount)
                .followers(user.getFollowersCount())
                .following(user.getFollowingCount())
                .build())
            .build();
    }
}
