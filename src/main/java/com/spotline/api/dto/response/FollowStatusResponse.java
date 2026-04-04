package com.spotline.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "팔로우 상태")
@Data
@AllArgsConstructor
public class FollowStatusResponse {

    @JsonProperty("isFollowing")
    private boolean following;
}
