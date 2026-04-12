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
    private List<DailyHour> dailyHours;
    private List<MenuItem> menuItems;
    private List<String> facilities;

    @Data
    @Builder
    public static class DailyHour {
        private String day;      // "월", "화", ... "일"
        private String timeSE;   // "10:00~22:00"
    }

    @Data
    @Builder
    public static class MenuItem {
        private String name;     // "아메리카노"
        private String price;    // "5,000"
        private String photo;    // nullable
    }
}
