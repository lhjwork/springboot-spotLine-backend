package com.spotline.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "소셜 상태 응답 (좋아요/저장/방문)")
@Data
@AllArgsConstructor
public class SocialStatusResponse {

    @JsonProperty("isLiked")
    private boolean liked;

    @JsonProperty("isSaved")
    private boolean saved;

    @JsonProperty("isVisited")
    private boolean visited;
}
