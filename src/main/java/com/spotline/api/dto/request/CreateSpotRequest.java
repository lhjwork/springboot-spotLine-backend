package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateSpotRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String description;

    @NotNull(message = "카테고리는 필수입니다")
    private SpotCategory category;

    @NotNull(message = "소스 타입은 필수입니다")
    private SpotSource source;

    private String crewNote;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @NotBlank(message = "지역은 필수입니다")
    private String area;

    private String sido;
    private String sigungu;
    private String dong;

    private String blogUrl;
    private String instagramUrl;
    private String websiteUrl;

    private String naverPlaceId;
    private String kakaoPlaceId;

    private List<String> tags;
    private List<String> media;
    private List<MediaItemRequest> mediaItems;

    private String creatorName;
}
