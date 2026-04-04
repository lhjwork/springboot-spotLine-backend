package com.spotline.api.dto.response;

import com.spotline.api.domain.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "S3 Presigned URL 응답")
@Data
@Builder
public class PresignedUrlResponse {
    private String uploadUrl;
    private String s3Key;
    private String publicUrl;
    private MediaType mediaType;
}
