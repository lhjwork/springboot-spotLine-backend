package com.spotline.api.infrastructure.place;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceApiServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private PlaceApiService placeApiService;

    @BeforeEach
    void setUp() {
        placeApiService = new PlaceApiService(webClientBuilder);
        ReflectionTestUtils.setField(placeApiService, "naverClientId", "test-naver-id");
        ReflectionTestUtils.setField(placeApiService, "naverClientSecret", "test-naver-secret");
        ReflectionTestUtils.setField(placeApiService, "kakaoRestApiKey", "test-kakao-key");
    }

    @Test
    @DisplayName("네이버 검색 결과 정규화 — HTML 태그 제거")
    void searchNaver_stripsHtmlFromTitle() {
        Map<String, Object> item = Map.of(
                "title", "<b>카페</b> 어니언",
                "roadAddress", "서울 성동구 성수이로 126",
                "telephone", "02-1234-5678",
                "category", "카페",
                "link", "https://naver.me/test123"
        );
        Map<String, Object> result = Map.of("items", List.of(item));

        setupWebClientMock(result);

        List<PlaceInfo> results = placeApiService.searchNaver("카페 어니언", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("카페 어니언"); // HTML 제거됨
        assertThat(results.get(0).getProvider()).isEqualTo("naver");
        assertThat(results.get(0).getAddress()).isEqualTo("서울 성동구 성수이로 126");
    }

    @Test
    @DisplayName("카카오 검색 결과 정규화")
    void searchKakao_normalizesResults() {
        Map<String, Object> doc = Map.of(
                "id", "12345",
                "place_name", "카페 어니언",
                "road_address_name", "서울 성동구 성수이로 126",
                "phone", "02-1234-5678",
                "category_name", "음식점 > 카페",
                "place_url", "https://place.map.kakao.com/12345"
        );
        Map<String, Object> result = Map.of("documents", List.of(doc));

        setupWebClientMock(result);

        List<PlaceInfo> results = placeApiService.searchKakao("카페 어니언", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPlaceId()).isEqualTo("12345");
        assertThat(results.get(0).getProvider()).isEqualTo("kakao");
        assertThat(results.get(0).getName()).isEqualTo("카페 어니언");
    }

    @Test
    @DisplayName("API 키 미설정 시 빈 목록 반환")
    void searchNaver_noApiKey_returnsEmpty() {
        ReflectionTestUtils.setField(placeApiService, "naverClientId", "");

        List<PlaceInfo> results = placeApiService.searchNaver("카페", 5);

        assertThat(results).isEmpty();
        verifyNoInteractions(webClientBuilder);
    }

    @Test
    @DisplayName("카카오 API 키 미설정 시 빈 목록 반환")
    void searchKakao_noApiKey_returnsEmpty() {
        ReflectionTestUtils.setField(placeApiService, "kakaoRestApiKey", "");

        List<PlaceInfo> results = placeApiService.searchKakao("카페", 5);

        assertThat(results).isEmpty();
        verifyNoInteractions(webClientBuilder);
    }

    @Test
    @DisplayName("Place API 호출 실패 시 빈 목록 반환 (graceful)")
    void searchNaver_apiFails_returnsEmpty() {
        given(webClientBuilder.build()).willReturn(webClient);
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willThrow(new RuntimeException("Connection refused"));

        List<PlaceInfo> results = placeApiService.searchNaver("카페", 5);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("getPlaceDetail - provider별 분기 처리")
    void getPlaceDetail_unknownProvider_returnsNull() {
        PlaceInfo result = placeApiService.getPlaceDetail("unknown", "12345");

        assertThat(result).isNull();
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMock(Map<String, Object> responseBody) {
        given(webClientBuilder.build()).willReturn(webClient);
        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(Map.class)).willReturn(Mono.just(responseBody));
    }
}
