package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "블로그 생성 요청")
public class CreateBlogRequest {

    @Schema(description = "연결할 SpotLine ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "SpotLine ID는 필수입니다")
    private UUID spotLineId;

    @Schema(description = "블로그 제목", example = "연남동 카페&와인 데이트")
    @NotBlank(message = "제목은 필수입니다")
    private String title;
}
