package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Schema(description = "인기 콘텐츠 응답")
@Data
@Builder
public class PopularContentResponse {
    private UUID id;
    private String slug;
    private String title;
    private String label;
    private Integer viewsCount;
    private Integer commentsCount;
}
