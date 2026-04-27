package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "컬렉션 수정 요청")
public class UpdateCollectionRequest {

    @Schema(description = "컬렉션 제목")
    private String title;

    @Schema(description = "설명")
    private String description;

    @Schema(description = "커버 이미지 URL")
    private String coverImageUrl;

    @Schema(description = "테마")
    private SpotLineTheme theme;

    @Schema(description = "지역")
    private String area;

    @Schema(description = "Featured 여부")
    private Boolean isFeatured;

    @Schema(description = "공개 여부")
    private Boolean isPublished;

    @Schema(description = "표시 순서")
    private Integer displayOrder;
}
