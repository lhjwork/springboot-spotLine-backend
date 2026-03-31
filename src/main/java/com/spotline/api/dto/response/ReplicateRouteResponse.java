package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.UserRoute;
import lombok.AllArgsConstructor;
import lombok.Data;

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
