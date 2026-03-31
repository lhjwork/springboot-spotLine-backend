package com.spotline.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AvatarUploadRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "Content-Type은 필수입니다")
    private String contentType;
}
