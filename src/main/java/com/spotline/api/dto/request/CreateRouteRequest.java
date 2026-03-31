package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.RouteTheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateRouteRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String description;

    @NotNull(message = "테마는 필수입니다")
    private RouteTheme theme;

    @NotBlank(message = "지역은 필수입니다")
    private String area;

    @NotEmpty(message = "Spot이 최소 1개 필요합니다")
    private List<RouteSpotRequest> spots;

    private UUID parentRouteId;
    private String creatorName;

    @Data
    public static class RouteSpotRequest {
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
