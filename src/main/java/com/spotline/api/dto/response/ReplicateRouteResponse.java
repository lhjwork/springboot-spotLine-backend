package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.UserRoute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "루트 복제 결과")
@Data
@AllArgsConstructor
public class ReplicateRouteResponse {
    private MyRouteResponse myRoute;
    private Integer replicationsCount;

    public static ReplicateRouteResponse from(UserRoute ur, Route route) {
        return new ReplicateRouteResponse(
            MyRouteResponse.from(ur),
            route.getReplicationsCount()
        );
    }
}
