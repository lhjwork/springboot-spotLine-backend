package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.BlogBlockType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "블로그 블록 일괄 저장 요청")
public class SaveBlogBlocksRequest {

    @NotEmpty(message = "블록 목록은 비어있을 수 없습니다")
    private List<BlockRequest> blocks;

    @Data
    public static class BlockRequest {

        @Schema(description = "기존 블록 ID (업데이트 시)")
        private UUID id;

        @Schema(description = "연결된 Spot ID (SPOT 블록일 때)")
        private UUID spotId;

        @NotNull(message = "블록 타입은 필수입니다")
        private BlogBlockType blockType;

        @NotNull(message = "블록 순서는 필수입니다")
        private Integer blockOrder;

        @Schema(description = "Tiptap JSON 콘텐츠")
        private String content;

        private List<MediaRequest> mediaItems;
    }

    @Data
    public static class MediaRequest {

        @Schema(description = "기존 미디어 ID")
        private UUID id;

        @NotNull(message = "미디어 URL은 필수입니다")
        private String mediaUrl;

        private Integer mediaOrder;

        private String caption;
    }
}
