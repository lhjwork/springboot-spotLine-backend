package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "아바타 업로드 결과")
@Data
@Builder
public class AvatarUploadResponse {
    private String presignedUrl;
    private String avatarKey;
    private String avatarUrl;
}
