package com.spotline.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowResponse {
    private boolean followed;
    private Integer followersCount;
}
