package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "체크인 통계 응답 (어드민)")
public class CheckinStatsResponse {
    private Long totalCheckins;
    private Long verifiedCheckins;
    private Long uniqueUsers;
    private Long uniqueSpots;
    private Double verificationRate;
    private Double avgCheckinsPerUser;
}
