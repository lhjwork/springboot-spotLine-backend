package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "체크인 목록 항목")
public class CheckinListResponse {
    private UUID id;
    private String userId;
    private String memo;
    private Boolean verified;
    private LocalDateTime createdAt;

    // Spot 체크인 목록에서만 사용
    private UUID spotId;
    private String spotTitle;
    private String spotSlug;
    private String spotThumbnail;
}
