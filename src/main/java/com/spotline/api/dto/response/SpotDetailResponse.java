package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotMedia;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.domain.enums.SpotStatus;
import com.spotline.api.infrastructure.place.PlaceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "스팟 상세 응답")
@Data
@Builder
public class SpotDetailResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private SpotCategory category;
    private SpotSource source;
    private String crewNote;

    // Location
    private String address;
    private Double latitude;
    private Double longitude;
    private String area;
    private String sido;
    private String sigungu;
    private String dong;

    // External Links
    private String blogUrl;
    private String instagramUrl;
    private String websiteUrl;

    // External Place API IDs
    private String naverPlaceId;
    private String kakaoPlaceId;

    private List<String> tags;
    private List<String> media;
    private List<SpotMediaResponse> mediaItems;

    // Stats
    private Integer likesCount;
    private Integer savesCount;
    private Integer viewsCount;
    private Integer commentsCount;

    // Creator
    private String creatorType;
    private String creatorName;

    private LocalDateTime createdAt;

    /** Place API에서 가져온 매장 상세 (nullable — API 실패 시 null) */
    private PlaceInfo placeInfo;

    // Approval Workflow
    private SpotStatus status;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private String reviewedBy;

    /** 파트너 정보 (파트너 매장인 경우에만, 아니면 null) */
    private SpotPartnerInfo partner;

    public static SpotDetailResponse from(Spot spot, PlaceInfo placeInfo) {
        return from(spot, placeInfo, null);
    }

    public static SpotDetailResponse from(Spot spot, PlaceInfo placeInfo, String s3BaseUrl, SpotPartnerInfo partnerInfo) {
        SpotDetailResponse response = from(spot, placeInfo, s3BaseUrl);
        response.setPartner(partnerInfo);
        return response;
    }

    public static SpotDetailResponse from(Spot spot, PlaceInfo placeInfo, String s3BaseUrl) {
        List<SpotMediaResponse> mediaResponses = null;
        if (s3BaseUrl != null && spot.getMediaItems() != null && !spot.getMediaItems().isEmpty()) {
            mediaResponses = spot.getMediaItems().stream()
                    .map(m -> SpotMediaResponse.from(m, s3BaseUrl))
                    .toList();
        }

        return SpotDetailResponse.builder()
                .id(spot.getId())
                .slug(spot.getSlug())
                .title(spot.getTitle())
                .description(spot.getDescription())
                .category(spot.getCategory())
                .source(spot.getSource())
                .crewNote(spot.getCrewNote())
                .address(spot.getAddress())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .area(spot.getArea())
                .sido(spot.getSido())
                .sigungu(spot.getSigungu())
                .dong(spot.getDong())
                .blogUrl(spot.getBlogUrl())
                .instagramUrl(spot.getInstagramUrl())
                .websiteUrl(spot.getWebsiteUrl())
                .naverPlaceId(spot.getNaverPlaceId())
                .kakaoPlaceId(spot.getKakaoPlaceId())
                .tags(spot.getTags())
                .media(spot.getMedia())
                .mediaItems(mediaResponses)
                .likesCount(spot.getLikesCount())
                .savesCount(spot.getSavesCount())
                .viewsCount(spot.getViewsCount())
                .commentsCount(spot.getCommentsCount())
                .creatorType(spot.getCreatorType())
                .creatorName(spot.getCreatorName())
                .createdAt(spot.getCreatedAt())
                .status(spot.getStatus())
                .rejectionReason(spot.getRejectionReason())
                .reviewedAt(spot.getReviewedAt())
                .reviewedBy(spot.getReviewedBy())
                .placeInfo(placeInfo)
                .build();
    }
}
