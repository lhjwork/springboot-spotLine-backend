package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.enums.BlogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "블로그 목록 아이템")
@Data
@Builder
public class BlogListItemResponse {
    private UUID id;
    private String slug;
    private String title;
    private String summary;
    private String coverImageUrl;
    private BlogStatus status;
    private String userName;
    private String userAvatarUrl;
    private String spotLineTitle;
    private String spotLineArea;
    private Integer spotCount;
    private Integer viewsCount;
    private Integer likesCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static BlogListItemResponse from(Blog blog) {
        return BlogListItemResponse.builder()
                .id(blog.getId())
                .slug(blog.getSlug())
                .title(blog.getTitle())
                .summary(blog.getSummary())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .userName(blog.getUserName())
                .userAvatarUrl(blog.getUserAvatarUrl())
                .spotLineTitle(blog.getSpotLine().getTitle())
                .spotLineArea(blog.getSpotLine().getArea())
                .spotCount(blog.getSpotLine().getSpots().size())
                .viewsCount(blog.getViewsCount())
                .likesCount(blog.getLikesCount())
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .build();
    }
}
