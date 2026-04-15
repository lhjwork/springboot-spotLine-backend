package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Top Spots 체크인 응답 (어드민)")
public class TopSpotCheckinResponse {

    private List<TopSpotItem> spots;

    @Data
    @Builder
    @AllArgsConstructor
    public static class TopSpotItem {
        private UUID spotId;
        private String spotTitle;
        private String area;
        private Long totalCheckins;
        private Long uniqueVisitors;
        private Double verifiedRate;
    }
}
