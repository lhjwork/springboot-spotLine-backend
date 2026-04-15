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
@Schema(description = "체크인 응답")
public class CheckinResponse {
    private UUID id;
    private UUID spotId;
    private Boolean verified;
    private String memo;
    private Integer visitedCount;
    private Long myCheckinCount;
    private LocalDateTime createdAt;
}
