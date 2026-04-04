package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "아바타 업로드 요청")
public class AvatarUploadRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "Content-Type은 필수입니다")
    private String contentType;
}
