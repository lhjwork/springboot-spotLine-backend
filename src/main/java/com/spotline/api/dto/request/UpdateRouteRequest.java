package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.RouteTheme;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRouteRequest {
    private String title;
    private String description;
    private RouteTheme theme;
    private String area;
    private List<CreateRouteRequest.RouteSpotRequest> spots; // null이면 spots 변경 안 함
}
