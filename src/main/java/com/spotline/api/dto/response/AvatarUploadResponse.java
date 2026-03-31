package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarUploadResponse {
    private String presignedUrl;
    private String avatarKey;
    private String avatarUrl;
}
