package com.spotline.api.dto.response;

import com.spotline.api.domain.enums.MediaType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignedUrlResponse {
    private String uploadUrl;
    private String s3Key;
    private String publicUrl;
    private MediaType mediaType;
}
