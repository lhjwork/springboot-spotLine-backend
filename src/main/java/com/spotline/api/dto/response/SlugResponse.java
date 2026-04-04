package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "Slug 응답 (SSR/sitemap)")
@Getter
@Builder
public class SlugResponse {
    private String slug;
    private LocalDateTime updatedAt;
}
