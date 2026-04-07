package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "블로그 수정 요청")
public class UpdateBlogRequest {

    @Schema(description = "블로그 제목")
    private String title;

    @Schema(description = "한줄 소개")
    private String summary;

    @Schema(description = "커버 이미지 URL")
    private String coverImageUrl;
}
