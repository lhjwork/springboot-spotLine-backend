package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "내 SpotLine 업데이트 요청")
public class UpdateMySpotLineStatusRequest {
    @Schema(description = "상태 변경", example = "completed")
    private String status;

    @Schema(description = "예정 날짜 변경", example = "2026-04-20")
    private String scheduledDate;
}
