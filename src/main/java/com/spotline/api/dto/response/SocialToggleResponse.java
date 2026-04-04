package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "소셜 토글 결과")
@Data
@AllArgsConstructor
public class SocialToggleResponse {
    private Boolean liked;
    private Boolean saved;
    private Integer likesCount;
    private Integer savesCount;
}
