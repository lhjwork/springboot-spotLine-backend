package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "컬렉션 생성 요청")
public class CreateCollectionRequest {

    @NotBlank
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

    @Schema(description = "생성자 이름")
    private String createdBy;

    @Schema(description = "초기 아이템 목록")
    private List<ItemRequest> items;

    @Data
    @Schema(description = "컬렉션 아이템 요청")
    public static class ItemRequest {
        @Schema(description = "Spot ID (spotLineId와 택1)")
        private UUID spotId;

        @Schema(description = "SpotLine ID (spotId와 택1)")
        private UUID spotLineId;

        @Schema(description = "아이템 순서")
        private Integer itemOrder;

        @Schema(description = "크루 한줄 추천")
        private String itemNote;
    }
}
