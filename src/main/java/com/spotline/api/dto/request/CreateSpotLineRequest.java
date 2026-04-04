package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "SpotLine 생성 요청")
public class CreateSpotLineRequest {
    @Schema(description = "SpotLine 제목", example = "연남동 카페 투어")
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @Schema(description = "SpotLine 설명")
    private String description;

    @Schema(description = "테마", example = "CAFE_TOUR")
    @NotNull(message = "테마는 필수입니다")
    private SpotLineTheme theme;

    @Schema(description = "지역", example = "연남/연희")
    @NotBlank(message = "지역은 필수입니다")
    private String area;

    @Schema(description = "스팟 목록 (순서대로)")
    @NotEmpty(message = "Spot이 최소 1개 필요합니다")
    private List<SpotLineSpotRequest> spots;

    private UUID parentSpotLineId;
    private String creatorName;

    @Data
    public static class SpotLineSpotRequest {
        @NotNull(message = "Spot ID는 필수입니다")
        private UUID spotId;
        private Integer order;
        private String suggestedTime;
        private Integer stayDuration;
        private Integer walkingTimeToNext;
        private Integer distanceToNext;
        private String transitionNote;
    }
}
