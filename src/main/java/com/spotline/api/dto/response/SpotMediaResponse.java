package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.MediaType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

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
