package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "콘텐츠 퍼포먼스 응답")
@Data
@Builder
public class ContentPerformanceResponse {
    private UUID id;
    private String slug;
    private String title;
    private String area;
    private String creatorName;
    private int viewsCount;
    private int likesCount;
    private int savesCount;
    private int commentsCount;
    private LocalDateTime createdAt;
}
