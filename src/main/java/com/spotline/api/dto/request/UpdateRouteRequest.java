package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.RouteTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "루트 수정 요청")
public class UpdateRouteRequest {
    private String title;
    private String description;
    private RouteTheme theme;
    private String area;
    private List<CreateRouteRequest.RouteSpotRequest> spots; // null이면 spots 변경 안 함
}
