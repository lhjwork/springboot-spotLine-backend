package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "루트 복제 요청")
public class ReplicateRouteRequest {
    private String scheduledDate;
}
