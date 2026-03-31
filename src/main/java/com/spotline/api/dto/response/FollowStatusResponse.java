package com.spotline.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowStatusResponse {

    @JsonProperty("isFollowing")
    private boolean following;
}
