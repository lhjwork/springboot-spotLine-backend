package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "SpotLine 복제 요청")
public class ReplicateSpotLineRequest {
    private String scheduledDate;
}
