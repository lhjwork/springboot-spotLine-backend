package com.spotline.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocialStatusResponse {

    @JsonProperty("isLiked")
    private boolean liked;

    @JsonProperty("isSaved")
    private boolean saved;
}
