package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.entity.SpotLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "팔로잉 피드 아이템")
@Data
@Builder
public class FollowingFeedItemResponse {

    public enum FeedItemType { SPOTLINE, BLOG }

    private FeedItemType type;
    private UUID id;
    private String slug;
    private String title;
    private String area;
    private String coverImageUrl;
    private Integer likesCount;
    private Integer viewsCount;

    // SpotLine specific
    private String theme;
    private Integer spotCount;
    private Integer totalDuration;

    // Blog specific
    private String summary;

    // Creator info
    private String userName;
    private String userAvatar;

    private LocalDateTime createdAt;

    public static FollowingFeedItemResponse fromSpotLine(SpotLine sl, String s3BaseUrl) {
        return FollowingFeedItemResponse.builder()
                .type(FeedItemType.SPOTLINE)
                .id(sl.getId())
                .slug(sl.getSlug())
                .title(sl.getTitle())
                .area(sl.getArea())
                .coverImageUrl(SpotLinePreviewResponse.from(sl, s3BaseUrl).getCoverImageUrl())
                .likesCount(sl.getLikesCount())
                .viewsCount(sl.getViewsCount())
                .theme(sl.getTheme() != null ? sl.getTheme().name() : null)
                .spotCount(sl.getSpots() != null ? sl.getSpots().size() : 0)
                .totalDuration(sl.getTotalDuration())
                .userName(sl.getCreatorName())
                .userAvatar(null)
                .createdAt(sl.getCreatedAt())
                .build();
    }

    public static FollowingFeedItemResponse fromBlog(Blog blog) {
        return FollowingFeedItemResponse.builder()
                .type(FeedItemType.BLOG)
                .id(blog.getId())
                .slug(blog.getSlug())
                .title(blog.getTitle())
                .area(blog.getSpotLine() != null ? blog.getSpotLine().getArea() : null)
                .coverImageUrl(blog.getCoverImageUrl())
                .likesCount(blog.getLikesCount())
                .viewsCount(blog.getViewsCount())
                .summary(blog.getSummary())
                .spotCount(blog.getSpotLine() != null && blog.getSpotLine().getSpots() != null
                        ? blog.getSpotLine().getSpots().size() : 0)
                .userName(blog.getUserName())
                .userAvatar(blog.getUserAvatarUrl())
                .createdAt(blog.getPublishedAt() != null ? blog.getPublishedAt() : blog.getCreatedAt())
                .build();
    }
}
