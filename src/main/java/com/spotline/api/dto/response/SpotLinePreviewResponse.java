package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Schema(description = "SpotLine 미리보기 응답")
@Data
@Builder
public class SpotLinePreviewResponse {
    private UUID id;
    private String slug;
    private String title;
    private SpotLineTheme theme;
    private String area;
    private Integer totalDuration;
    private Integer totalDistance;
    private Integer spotCount;
    private Integer likesCount;
    private String coverImageUrl;

    public static SpotLinePreviewResponse from(SpotLine spotLine) {
        return from(spotLine, null);
    }

    public static SpotLinePreviewResponse from(SpotLine spotLine, String s3BaseUrl) {
        return SpotLinePreviewResponse.builder()
                .id(spotLine.getId())
                .slug(spotLine.getSlug())
                .title(spotLine.getTitle())
                .theme(spotLine.getTheme())
                .area(spotLine.getArea())
                .totalDuration(spotLine.getTotalDuration())
                .totalDistance(spotLine.getTotalDistance())
                .spotCount(spotLine.getSpots() != null ? spotLine.getSpots().size() : 0)
                .likesCount(spotLine.getLikesCount())
                .coverImageUrl(resolveCoverImageUrl(spotLine, s3BaseUrl))
                .build();
    }

    private static String resolveCoverImageUrl(SpotLine spotLine, String s3BaseUrl) {
        if (s3BaseUrl == null || spotLine.getSpots() == null || spotLine.getSpots().isEmpty()) {
            return null;
        }
        Spot firstSpot = spotLine.getSpots().get(0).getSpot();
        if (firstSpot == null) return null;

        if (firstSpot.getMediaItems() != null && !firstSpot.getMediaItems().isEmpty()) {
            SpotMedia firstMedia = firstSpot.getMediaItems().get(0);
            String key = firstMedia.getThumbnailS3Key() != null
                    ? firstMedia.getThumbnailS3Key()
                    : firstMedia.getS3Key();
            return s3BaseUrl + "/" + key;
        }

        if (firstSpot.getMedia() != null && !firstSpot.getMedia().isEmpty()) {
            return s3BaseUrl + "/" + firstSpot.getMedia().get(0);
        }

        return null;
    }
}
