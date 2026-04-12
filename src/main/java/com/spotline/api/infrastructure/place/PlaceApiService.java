package com.spotline.api.infrastructure.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceApiService {

    private final WebClient.Builder webClientBuilder;

    @Value("${place.naver.client-id:}")
    private String naverClientId;

    @Value("${place.naver.client-secret:}")
    private String naverClientSecret;

    @Value("${place.kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    /**
     * 네이버 Place 검색 (크루 큐레이션 도구용)
     */
    public List<PlaceInfo> searchNaver(String query, int display) {
        if (naverClientId.isEmpty()) {
            log.warn("네이버 Place API 키가 설정되지 않았습니다");
            return Collections.emptyList();
        }

        try {
            Map<?, ?> result = webClientBuilder.build()
                    .get()
                    .uri("https://openapi.naver.com/v1/search/local.json?query={query}&display={display}",
                            query, display)
                    .header("X-Naver-Client-Id", naverClientId)
                    .header("X-Naver-Client-Secret", naverClientSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result == null || !result.containsKey("items")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

            return items.stream()
                    .map(item -> PlaceInfo.builder()
                            .provider("naver")
                            .placeId(String.valueOf(item.getOrDefault("link", "")))
                            .name(stripHtml(String.valueOf(item.getOrDefault("title", ""))))
                            .address(String.valueOf(item.getOrDefault("roadAddress",
                                    item.getOrDefault("address", ""))))
                            .phone(String.valueOf(item.getOrDefault("telephone", "")))
                            .category(String.valueOf(item.getOrDefault("category", "")))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("네이버 Place API 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 카카오 Place 검색 (크루 큐레이션 도구용)
     */
    public List<PlaceInfo> searchKakao(String query, int size) {
        if (kakaoRestApiKey.isEmpty()) {
            log.warn("카카오 Place API 키가 설정되지 않았습니다");
            return Collections.emptyList();
        }

        try {
            Map<?, ?> result = webClientBuilder.build()
                    .get()
                    .uri("https://dapi.kakao.com/v2/local/search/keyword.json?query={query}&size={size}",
                            query, size)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result == null || !result.containsKey("documents")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> docs = (List<Map<String, Object>>) result.get("documents");

            return docs.stream()
                    .map(doc -> PlaceInfo.builder()
                            .provider("kakao")
                            .placeId(String.valueOf(doc.getOrDefault("id", "")))
                            .name(String.valueOf(doc.getOrDefault("place_name", "")))
                            .address(String.valueOf(doc.getOrDefault("road_address_name",
                                    doc.getOrDefault("address_name", ""))))
                            .phone(String.valueOf(doc.getOrDefault("phone", "")))
                            .category(String.valueOf(doc.getOrDefault("category_name", "")))
                            .url(String.valueOf(doc.getOrDefault("place_url", "")))
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("카카오 Place API 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Place 상세 조회 + 캐싱 (Spot 페이지 렌더링용)
     * 캐시 키: place:{provider}:{placeId}, TTL 24h
     */
    @Cacheable(value = "placeInfo", key = "#provider + ':' + #placeId")
    public PlaceInfo getPlaceDetail(String provider, String placeId) {
        log.info("Place API 호출 (캐시 미스): provider={}, placeId={}", provider, placeId);

        try {
            return switch (provider) {
                case "kakao" -> fetchKakaoDetail(placeId);
                case "naver" -> fetchNaverDetail(placeId);
                default -> null;
            };
        } catch (Exception e) {
            log.error("Place API 상세 조회 실패: provider={}, placeId={}, error={}",
                    provider, placeId, e.getMessage());
            return null; // graceful degradation
        }
    }

    @SuppressWarnings("unchecked")
    private PlaceInfo fetchKakaoDetail(String placeId) {
        if (kakaoRestApiKey.isEmpty()) return null;

        // 카카오 Place 상세 API (place.map.kakao.com)
        try {
            Map<?, ?> result = webClientBuilder.build()
                    .get()
                    .uri("https://place.map.kakao.com/main/v/" + placeId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result == null || !result.containsKey("basicInfo")) {
                log.debug("카카오 상세 조회 결과 없음: placeId={}", placeId);
                return null;
            }

            Map<String, Object> basicInfo = (Map<String, Object>) result.get("basicInfo");

            String name = String.valueOf(basicInfo.getOrDefault("placenamefull", ""));
            String address = "";
            if (basicInfo.containsKey("address")) {
                Map<String, Object> addr = (Map<String, Object>) basicInfo.get("address");
                address = String.valueOf(addr.getOrDefault("newaddr", addr.getOrDefault("addrbunho", "")));
                String region = String.valueOf(addr.getOrDefault("region", Map.of()));
                if (addr.containsKey("region")) {
                    Map<String, Object> regionMap = (Map<String, Object>) addr.get("region");
                    address = String.valueOf(regionMap.getOrDefault("fullname", "")) + " " + address;
                }
            }

            String phone = String.valueOf(basicInfo.getOrDefault("phonenum", ""));
            String category = String.valueOf(basicInfo.getOrDefault("category", Map.of()));
            if (basicInfo.get("category") instanceof Map) {
                Map<String, Object> cat = (Map<String, Object>) basicInfo.get("category");
                category = String.valueOf(cat.getOrDefault("catename", ""));
            }

            String businessHours = "";
            List<PlaceInfo.DailyHour> dailyHours = new ArrayList<>();
            if (basicInfo.containsKey("openHour")) {
                Map<String, Object> openHour = (Map<String, Object>) basicInfo.get("openHour");
                if (openHour.containsKey("periodList")) {
                    List<Map<String, Object>> periods = (List<Map<String, Object>>) openHour.get("periodList");
                    if (!periods.isEmpty() && periods.get(0).containsKey("timeList")) {
                        List<Map<String, Object>> times = (List<Map<String, Object>>) periods.get(0).get("timeList");
                        if (!times.isEmpty()) {
                            businessHours = String.valueOf(times.get(0).getOrDefault("timeSE", ""));
                        }
                        for (Map<String, Object> time : times) {
                            dailyHours.add(PlaceInfo.DailyHour.builder()
                                    .day(String.valueOf(time.getOrDefault("timeName", "")))
                                    .timeSE(String.valueOf(time.getOrDefault("timeSE", "")))
                                    .build());
                        }
                    }
                }
            }

            // 메뉴 목록
            List<PlaceInfo.MenuItem> menuItems = Collections.emptyList();
            if (result.containsKey("menuInfo") && result.get("menuInfo") instanceof Map) {
                Map<String, Object> menuInfo = (Map<String, Object>) result.get("menuInfo");
                if (menuInfo.containsKey("menuList")) {
                    List<Map<String, Object>> menuList = (List<Map<String, Object>>) menuInfo.get("menuList");
                    menuItems = menuList.stream()
                            .limit(10)
                            .map(m -> PlaceInfo.MenuItem.builder()
                                    .name(String.valueOf(m.getOrDefault("menu", "")))
                                    .price(String.valueOf(m.getOrDefault("price", "")))
                                    .photo(m.containsKey("img") ? String.valueOf(m.get("img")) : null)
                                    .build())
                            .toList();
                }
            }

            // 편의시설
            List<String> facilities = Collections.emptyList();
            if (basicInfo.containsKey("facilityInfo")) {
                Map<String, Object> facilityInfo = (Map<String, Object>) basicInfo.get("facilityInfo");
                facilities = facilityInfo.entrySet().stream()
                        .filter(e -> "Y".equals(String.valueOf(e.getValue())))
                        .map(Map.Entry::getKey)
                        .toList();
            }

            // 사진 목록
            List<String> photos = Collections.emptyList();
            if (result.containsKey("photo") && result.get("photo") instanceof Map) {
                Map<String, Object> photoSection = (Map<String, Object>) result.get("photo");
                if (photoSection.containsKey("photoList")) {
                    List<Map<String, Object>> photoList = (List<Map<String, Object>>) photoSection.get("photoList");
                    photos = photoList.stream()
                            .map(p -> String.valueOf(p.getOrDefault("orgurl", "")))
                            .filter(url -> !url.isEmpty())
                            .limit(5)
                            .toList();
                }
            }

            return PlaceInfo.builder()
                    .provider("kakao")
                    .placeId(placeId)
                    .name(name)
                    .address(address.trim())
                    .phone(phone)
                    .category(category)
                    .businessHours(businessHours)
                    .dailyHours(dailyHours.isEmpty() ? null : dailyHours)
                    .menuItems(menuItems.isEmpty() ? null : menuItems)
                    .facilities(facilities.isEmpty() ? null : facilities)
                    .photos(photos)
                    .url("https://place.map.kakao.com/" + placeId)
                    .build();
        } catch (Exception e) {
            log.warn("카카오 상세 조회 실패 (fallback): placeId={}, error={}", placeId, e.getMessage());
            return PlaceInfo.builder()
                    .provider("kakao")
                    .placeId(placeId)
                    .url("https://place.map.kakao.com/" + placeId)
                    .build();
        }
    }

    private PlaceInfo fetchNaverDetail(String placeId) {
        if (naverClientId.isEmpty()) return null;

        // 네이버 검색 API로 placeId(link) 기반 재검색 후 매칭
        // 네이버 Place 상세 공식 API는 없으므로, 저장된 검색 데이터 기반 최소 정보 반환
        try {
            return PlaceInfo.builder()
                    .provider("naver")
                    .placeId(placeId)
                    .url(placeId.startsWith("http") ? placeId : "https://map.naver.com/p/entry/place/" + placeId)
                    .build();
        } catch (Exception e) {
            log.warn("네이버 상세 조회 실패: placeId={}, error={}", placeId, e.getMessage());
            return null;
        }
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "");
    }
}
