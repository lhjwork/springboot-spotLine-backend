package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "스팟 생성 요청")
public class CreateSpotRequest {
    @Schema(description = "업체명", example = "바모스커피 연남점", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String description;

    @Schema(description = "카테고리", example = "CAFE")
    @NotNull(message = "카테고리는 필수입니다")
    private SpotCategory category;

    @Schema(description = "출처", example = "CREW")
    @NotNull(message = "소스 타입은 필수입니다")
    private SpotSource source;

    @Schema(description = "크루 한줄 추천", example = "연남동 최고의 루프탑 뷰")
    private String crewNote;

    @Schema(description = "도로명 주소", example = "서울 마포구 연남로 123")
    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @Schema(description = "위도", example = "37.5665")
    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @Schema(description = "지역", example = "연남/연희")
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

    @Schema(description = "태그 목록", example = "[\"뷰맛집\", \"루프탑\", \"브런치\"]")
    private List<String> tags;
    private List<String> media;
    @Schema(description = "미디어 아이템")
    private List<MediaItemRequest> mediaItems;

    @Schema(description = "작성자명", example = "crew")
    private String creatorName;
}
