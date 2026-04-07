package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.BlogBlock;
import com.spotline.api.domain.entity.BlogBlockMedia;
import com.spotline.api.domain.enums.BlogBlockType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "블로그 블록 응답")
@Data
@Builder
public class BlogBlockResponse {
    private UUID id;
    private UUID spotId;
    private BlogBlockType blockType;
    private Integer blockOrder;
    private String content;
    private List<BlogBlockMediaResponse> mediaItems;

    // Spot 정보 (SPOT 블록일 때)
    private String spotTitle;
    private String spotCategory;
    private String spotArea;
    private String spotAddress;
    private List<String> spotMedia;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class BlogBlockMediaResponse {
        private UUID id;
        private String mediaUrl;
        private Integer mediaOrder;
        private String caption;
    }

    public static BlogBlockResponse from(BlogBlock block) {
        BlogBlockResponseBuilder builder = BlogBlockResponse.builder()
                .id(block.getId())
                .spotId(block.getSpot() != null ? block.getSpot().getId() : null)
                .blockType(block.getBlockType())
                .blockOrder(block.getBlockOrder())
                .content(block.getContent())
                .mediaItems(block.getMediaItems().stream()
                        .map(BlogBlockResponse::mapMedia)
                        .toList())
                .createdAt(block.getCreatedAt())
                .updatedAt(block.getUpdatedAt());

        if (block.getSpot() != null) {
            builder.spotTitle(block.getSpot().getTitle())
                    .spotCategory(block.getSpot().getCategory().name())
                    .spotArea(block.getSpot().getArea())
                    .spotAddress(block.getSpot().getAddress())
                    .spotMedia(block.getSpot().getMedia());
        }

        return builder.build();
    }

    private static BlogBlockMediaResponse mapMedia(BlogBlockMedia media) {
        return BlogBlockMediaResponse.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .mediaOrder(media.getMediaOrder())
                .caption(media.getCaption())
                .build();
    }
}
