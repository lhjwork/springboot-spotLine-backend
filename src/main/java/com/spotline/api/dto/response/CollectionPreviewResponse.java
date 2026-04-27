package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "컬렉션 미리보기 응답")
public class CollectionPreviewResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String coverImageUrl;
    private String theme;
    private String area;
    private Integer itemCount;
    private Long viewsCount;

    public static CollectionPreviewResponse from(Collection c) {
        return CollectionPreviewResponse.builder()
                .id(c.getId()).slug(c.getSlug()).title(c.getTitle())
                .description(c.getDescription())
                .coverImageUrl(c.getCoverImageUrl())
                .theme(c.getTheme() != null ? c.getTheme().name() : null)
                .area(c.getArea())
                .itemCount(c.getItemCount()).viewsCount(c.getViewsCount())
                .build();
    }
}
