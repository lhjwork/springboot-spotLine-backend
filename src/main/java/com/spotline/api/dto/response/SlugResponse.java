package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SlugResponse {
    private String slug;
    private LocalDateTime updatedAt;
}
