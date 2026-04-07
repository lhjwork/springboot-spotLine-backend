package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Blog;
import com.spotline.api.domain.enums.BlogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "블로그 상세 응답")
@Data
@Builder
public class BlogDetailResponse {
    private UUID id;
    private String slug;
    private UUID spotLineId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String title;
    private String summary;
    private String coverImageUrl;
    private BlogStatus status;
    private Integer viewsCount;
    private Integer likesCount;
    private Integer savesCount;
    private Integer commentsCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SpotLineDetailResponse spotLine;
    private List<BlogBlockResponse> blocks;

    public static BlogDetailResponse from(Blog blog) {
        return BlogDetailResponse.builder()
                .id(blog.getId())
                .slug(blog.getSlug())
                .spotLineId(blog.getSpotLine().getId())
                .userId(blog.getUserId())
                .userName(blog.getUserName())
                .userAvatarUrl(blog.getUserAvatarUrl())
                .title(blog.getTitle())
                .summary(blog.getSummary())
                .coverImageUrl(blog.getCoverImageUrl())
                .status(blog.getStatus())
                .viewsCount(blog.getViewsCount())
                .likesCount(blog.getLikesCount())
                .savesCount(blog.getSavesCount())
                .commentsCount(blog.getCommentsCount())
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .spotLine(SpotLineDetailResponse.from(blog.getSpotLine()))
                .blocks(blog.getBlocks().stream()
                        .map(BlogBlockResponse::from)
                        .toList())
                .build();
    }
}
