package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PopularContentResponse {
    private UUID id;
    private String slug;
    private String title;
    private String label;
    private Integer viewsCount;
    private Integer commentsCount;
}
