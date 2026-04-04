package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "내 루트 상태 변경 요청")
public class UpdateMyRouteStatusRequest {
    private String status;
}
