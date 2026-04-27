package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Collection;
import com.spotline.api.domain.entity.CollectionItem;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "컬렉션 상세 응답")
public class CollectionDetailResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String coverImageUrl;
    private String theme;
    private String area;
    private Boolean isFeatured;
    private Boolean isPublished;
    private Integer displayOrder;
    private Long viewsCount;
    private Integer itemCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CollectionItemDetail> items;

    @Data
    @Builder
    @Schema(description = "컬렉션 아이템 상세")
    public static class CollectionItemDetail {
        private UUID id;
        private String itemType;
        private Integer itemOrder;
        private String itemNote;

        // Spot fields
        private UUID spotId;
        private String spotSlug;
        private String spotTitle;
        private String spotCategory;
        private String spotArea;
        private String spotCoverImage;

        // SpotLine fields
        private UUID spotLineId;
        private String spotLineSlug;
        private String spotLineTitle;
        private String spotLineTheme;
        private String spotLineArea;
        private Integer spotLineSpotCount;
        private String spotLineCoverImage;
    }

    public static CollectionDetailResponse from(Collection c) {
        List<CollectionItemDetail> itemDetails = c.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                .map(CollectionDetailResponse::mapItem)
                .toList();

        return CollectionDetailResponse.builder()
                .id(c.getId()).slug(c.getSlug()).title(c.getTitle())
                .description(c.getDescription()).coverImageUrl(c.getCoverImageUrl())
                .theme(c.getTheme() != null ? c.getTheme().name() : null)
                .area(c.getArea())
                .isFeatured(c.getIsFeatured()).isPublished(c.getIsPublished())
                .displayOrder(c.getDisplayOrder())
                .viewsCount(c.getViewsCount()).itemCount(c.getItemCount())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .items(itemDetails)
                .build();
    }

    private static CollectionItemDetail mapItem(CollectionItem ci) {
        var b = CollectionItemDetail.builder()
                .id(ci.getId()).itemOrder(ci.getItemOrder()).itemNote(ci.getItemNote());

        if (ci.getSpot() != null) {
            Spot s = ci.getSpot();
            b.itemType("SPOT").spotId(s.getId()).spotSlug(s.getSlug())
                    .spotTitle(s.getTitle())
                    .spotCategory(s.getCategory() != null ? s.getCategory().name() : null)
                    .spotArea(s.getArea())
                    .spotCoverImage(s.getMedia() != null && !s.getMedia().isEmpty() ? s.getMedia().get(0) : null);
        } else if (ci.getSpotLine() != null) {
            SpotLine sl = ci.getSpotLine();
            b.itemType("SPOTLINE").spotLineId(sl.getId()).spotLineSlug(sl.getSlug())
                    .spotLineTitle(sl.getTitle())
                    .spotLineTheme(sl.getTheme() != null ? sl.getTheme().name() : null)
                    .spotLineArea(sl.getArea())
                    .spotLineSpotCount(sl.getSpots() != null ? sl.getSpots().size() : 0);
        }
        return b.build();
    }
}
