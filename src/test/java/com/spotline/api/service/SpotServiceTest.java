package com.spotline.api.service;

import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreateSpotRequest;
import com.spotline.api.dto.request.UpdateSpotRequest;
import com.spotline.api.domain.enums.FeedSort;
import com.spotline.api.dto.response.SpotDetailResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import com.spotline.api.infrastructure.place.PlaceApiService;
import com.spotline.api.infrastructure.place.PlaceInfo;
import com.spotline.api.infrastructure.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotServiceTest {

    @Mock
    private SpotRepository spotRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private PlaceApiService placeApiService;
    @Mock
    private S3Service s3Service;
    @Mock
    private PartnerService partnerService;
    @InjectMocks
    private SpotService spotService;

    private Spot testSpot;

    @BeforeEach
    void setUp() {
        lenient().when(s3Service.getPublicUrl("")).thenReturn("https://test-bucket.s3.ap-northeast-2.amazonaws.com/");

        testSpot = Spot.builder()
                .id(UUID.randomUUID())
                .slug("seongsu-cafe-onion")
                .title("성수 카페 어니언")
                .category(SpotCategory.CAFE)
                .source(SpotSource.CREW)
                .crewNote("빵이 맛있어요")
                .address("서울 성동구 성수이로 126")
                .latitude(37.5447)
                .longitude(127.0556)
                .area("성수")
                .kakaoPlaceId("12345")
                .tags(new ArrayList<>(List.of("카페", "브런치")))
                .media(new ArrayList<>())
                .creatorType("crew")
                .creatorName("Spotline Crew")
                .isActive(true)
                .likesCount(0)
                .savesCount(0)
                .viewsCount(0)
                .build();
    }

    @Test
    @DisplayName("slug로 Spot 상세 조회 시 PlaceInfo 병합 응답")
    void getBySlug_returnsSpotWithPlaceInfo() {
        given(spotRepository.findBySlugAndIsActiveTrue("seongsu-cafe-onion"))
                .willReturn(Optional.of(testSpot));

        PlaceInfo placeInfo = PlaceInfo.builder()
                .provider("kakao")
                .placeId("12345")
                .name("카페 어니언")
                .phone("02-1234-5678")
                .build();
        given(placeApiService.getPlaceDetail("kakao", "12345")).willReturn(placeInfo);

        SpotDetailResponse result = spotService.getBySlug("seongsu-cafe-onion");

        assertThat(result.getSlug()).isEqualTo("seongsu-cafe-onion");
        assertThat(result.getTitle()).isEqualTo("성수 카페 어니언");
        assertThat(result.getPlaceInfo()).isNotNull();
        assertThat(result.getPlaceInfo().getPhone()).isEqualTo("02-1234-5678");
    }

    @Test
    @DisplayName("존재하지 않는 slug 조회 시 ResourceNotFoundException")
    void getBySlug_notFound_throwsException() {
        given(spotRepository.findBySlugAndIsActiveTrue("invalid-slug"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> spotService.getBySlug("invalid-slug"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spot");
    }

    @Test
    @DisplayName("Place API 실패 시 placeInfo는 null (graceful)")
    void getBySlug_placeApiFails_returnsNullPlaceInfo() {
        given(spotRepository.findBySlugAndIsActiveTrue("seongsu-cafe-onion"))
                .willReturn(Optional.of(testSpot));
        given(placeApiService.getPlaceDetail("kakao", "12345")).willReturn(null);

        SpotDetailResponse result = spotService.getBySlug("seongsu-cafe-onion");

        assertThat(result.getSlug()).isEqualTo("seongsu-cafe-onion");
        assertThat(result.getPlaceInfo()).isNull();
    }

    @Test
    @DisplayName("Spot 생성 시 slug 자동 생성")
    void create_generatesSlug() {
        CreateSpotRequest request = new CreateSpotRequest();
        request.setTitle("새로운 카페");
        request.setCategory(SpotCategory.CAFE);
        request.setSource(SpotSource.CREW);
        request.setAddress("서울 성동구");
        request.setLatitude(37.5);
        request.setLongitude(127.0);
        request.setArea("성수");
        request.setCreatorName("Crew");

        given(spotRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willAnswer(inv -> {
            Spot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SpotDetailResponse result = spotService.create(request, null, "crew");

        assertThat(result.getTitle()).isEqualTo("새로운 카페");
        verify(spotRepository).save(any(Spot.class));
    }

    @Test
    @DisplayName("Spot 수정 - 부분 업데이트")
    void update_partialUpdate() {
        given(spotRepository.findBySlugAndIsActiveTrue("seongsu-cafe-onion"))
                .willReturn(Optional.of(testSpot));

        UpdateSpotRequest request = new UpdateSpotRequest();
        request.setCrewNote("새로운 크루노트");
        request.setArea("을지로");

        SpotDetailResponse result = spotService.update("seongsu-cafe-onion", request, null);

        assertThat(result.getCrewNote()).isEqualTo("새로운 크루노트");
        assertThat(result.getArea()).isEqualTo("을지로");
        assertThat(result.getTitle()).isEqualTo("성수 카페 어니언"); // 변경 안 됨
    }

    @Test
    @DisplayName("Spot 삭제 - soft delete (isActive=false)")
    void delete_softDelete() {
        given(spotRepository.findBySlugAndIsActiveTrue("seongsu-cafe-onion"))
                .willReturn(Optional.of(testSpot));
        given(spotRepository.save(any(Spot.class))).willAnswer(inv -> inv.getArgument(0));

        spotService.delete("seongsu-cafe-onion", null);

        assertThat(testSpot.getIsActive()).isFalse();
        verify(spotRepository).save(testSpot);
    }

    @Test
    @DisplayName("Spot 목록 조회 - area 필터")
    void list_filteredByArea() {
        Page<Spot> page = new PageImpl<>(List.of(testSpot));
        given(spotRepository.findByAreaAndIsActiveTrueOrderByViewsCountDesc("성수", PageRequest.of(0, 20)))
                .willReturn(page);

        Page<SpotDetailResponse> result = spotService.list("성수", null, FeedSort.POPULAR, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getArea()).isEqualTo("성수");
    }

    @Test
    @DisplayName("Spot 생성 시 중복 slug → suffix 추가 (-1, -2)")
    void create_duplicateSlug_addsSuffix() {
        CreateSpotRequest request = new CreateSpotRequest();
        request.setTitle("Test Cafe"); // slug = "test-cafe"
        request.setCategory(SpotCategory.CAFE);
        request.setSource(SpotSource.CREW);
        request.setAddress("서울 성동구");
        request.setLatitude(37.5);
        request.setLongitude(127.0);
        request.setArea("성수");
        request.setCreatorName("Crew");

        // 첫 slug 중복, 두 번째도 중복, 세 번째 통과
        given(spotRepository.existsBySlug("test-cafe")).willReturn(true);
        given(spotRepository.existsBySlug("test-cafe-1")).willReturn(true);
        given(spotRepository.existsBySlug("test-cafe-2")).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willAnswer(inv -> {
            Spot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SpotDetailResponse result = spotService.create(request, null, "crew");

        // slug 생성 시 existsBySlug가 호출되어 중복 확인
        verify(spotRepository).existsBySlug("test-cafe");
        verify(spotRepository).existsBySlug("test-cafe-1");
        verify(spotRepository).existsBySlug("test-cafe-2");
    }

    @Test
    @DisplayName("근처 Spot 검색 - 위경도 범위 계산 후 조회")
    void findNearby_calculatesLatLngBounds() {
        double lat = 37.5447;
        double lng = 127.0556;
        double radiusKm = 1.0;

        Spot nearbySpot = Spot.builder()
                .id(UUID.randomUUID())
                .slug("nearby-cafe")
                .title("근처 카페")
                .category(SpotCategory.CAFE)
                .source(SpotSource.CREW)
                .area("성수")
                .latitude(37.5450)
                .longitude(127.0560)
                .tags(new ArrayList<>())
                .media(new ArrayList<>())
                .isActive(true)
                .likesCount(0).savesCount(0).viewsCount(0)
                .build();

        given(spotRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(nearbySpot));

        List<SpotDetailResponse> results = spotService.findNearby(lat, lng, radiusKm);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSlug()).isEqualTo("nearby-cafe");

        // 위경도 범위가 올바르게 계산되었는지 확인
        double expectedLatDelta = radiusKm / 111.0;
        double expectedLngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        verify(spotRepository).findNearby(
                eq(lat - expectedLatDelta),
                eq(lat + expectedLatDelta),
                eq(lng - expectedLngDelta),
                eq(lng + expectedLngDelta));
    }
}
