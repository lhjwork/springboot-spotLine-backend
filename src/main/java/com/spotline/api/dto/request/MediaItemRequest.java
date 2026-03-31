package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MediaItemRequest {

    @NotBlank(message = "S3 키는 필수입니다")
    private String s3Key;

    @NotNull(message = "미디어 타입은 필수입니다")
    private MediaType mediaType;

    private String thumbnailS3Key;
    private Integer durationSec;
    private Integer displayOrder;
    private Long fileSizeBytes;
    private String mimeType;
}
