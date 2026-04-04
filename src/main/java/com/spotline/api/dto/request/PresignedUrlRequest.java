package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "S3 업로드 Presigned URL 요청")
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "Content-Type은 필수입니다")
    private String contentType;

    @NotNull(message = "파일 크기는 필수입니다")
    @Positive(message = "파일 크기는 양수여야 합니다")
    private Long contentLength;

    private Integer durationSec;
}
