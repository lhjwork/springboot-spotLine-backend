package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.TimeOfDay;
import com.spotline.api.domain.enums.WeatherCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "스팟 수정 요청")
public class UpdateSpotRequest {
    private String title;
    private String description;
    private SpotCategory category;
    private String crewNote;
    private String address;
    private Double latitude;
    private Double longitude;
    private String area;
    private String naverPlaceId;
    private String kakaoPlaceId;
    private List<String> tags;
    private List<String> media;
    private List<MediaItemRequest> mediaItems;

    private TimeOfDay bestTimeOfDay;
    private WeatherCondition bestWeatherCondition;
    private Boolean isIndoor;
}
