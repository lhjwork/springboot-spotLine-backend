package com.spotline.api.infrastructure.place;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlaceInfo {
    private String provider; // "naver" or "kakao"
    private String placeId;
    private String name;
    private String address;
    private String phone;
    private String category;
    private String businessHours;
    private Double rating;
    private Integer reviewCount;
    private List<String> photos;
    private String url; // 네이버/카카오 장소 페이지 URL
}
