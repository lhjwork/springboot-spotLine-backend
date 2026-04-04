package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Schema(description = "스팟 미디어 응답")
@Data
@Builder
public class SpotMediaResponse {
    private UUID id;
    private String url;
    private MediaType mediaType;
    private String thumbnailUrl;
    private Integer durationSec;
    private Integer displayOrder;

    public static SpotMediaResponse from(SpotMedia media, String baseUrl) {
        return SpotMediaResponse.builder()
                .id(media.getId())
                .url(baseUrl + "/" + media.getS3Key())
                .mediaType(media.getMediaType())
                .thumbnailUrl(media.getThumbnailS3Key() != null
                        ? baseUrl + "/" + media.getThumbnailS3Key() : null)
                .durationSec(media.getDurationSec())
                .displayOrder(media.getDisplayOrder())
                .build();
    }
}
