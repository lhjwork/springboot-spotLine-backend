package com.spotline.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SocialToggleResponse {
    private Boolean liked;
    private Boolean saved;
    private Integer likesCount;
    private Integer savesCount;
}
