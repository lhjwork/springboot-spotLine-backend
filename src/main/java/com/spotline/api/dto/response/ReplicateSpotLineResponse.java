package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserSpotLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "SpotLine 복제 결과")
@Data
@AllArgsConstructor
public class ReplicateSpotLineResponse {
    private MySpotLineResponse mySpotLine;
    private Integer replicationsCount;

    public static ReplicateSpotLineResponse from(UserSpotLine ur, SpotLine spotLine) {
        return new ReplicateSpotLineResponse(
            MySpotLineResponse.from(ur),
            spotLine.getReplicationsCount()
        );
    }
}
