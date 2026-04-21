package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopSharedContentResponse {
    private UUID targetId;
    private String targetType;
    private String title;
    private String slug;
    private long shareCount;
}
