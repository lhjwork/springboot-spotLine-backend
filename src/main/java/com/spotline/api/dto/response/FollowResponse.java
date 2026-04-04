package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "팔로우 결과")
@Data
@AllArgsConstructor
public class FollowResponse {
    private boolean followed;
    private Integer followersCount;
}
