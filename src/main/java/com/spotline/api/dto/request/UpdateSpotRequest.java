package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotCategory;
import lombok.Data;

import java.util.List;

@Data
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
}
