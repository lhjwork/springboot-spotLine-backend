package com.spotline.api.service;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.RouteTheme;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreateRouteRequest;
import com.spotline.api.dto.response.RouteDetailResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private SpotRepository spotRepository;
    @InjectMocks
    private RouteService routeService;

    private Spot spot1;
    private Spot spot2;
    private Spot spot3;

    @BeforeEach
    void setUp() {
        spot1 = Spot.builder()
                .id(UUID.randomUUID())
                .slug("cafe-onion")
                .title("카페 어니언")
                .category(SpotCategory.CAFE)
                .source(SpotSource.CREW)
                .address("서울 성동구")
                .latitude(37.5447)
                .longitude(127.0556)
                .area("성수")
                .tags(new ArrayList<>())
                .media(new ArrayList<>())
                .creatorType("crew")
                .isActive(true)
                .likesCount(0).savesCount(0).viewsCount(0)
                .build();

        spot2 = Spot.builder()
                .id(UUID.randomUUID())
                .slug("seongsu-restaurant")
                .title("성수 레스토랑")
                .category(SpotCategory.RESTAURANT)
                .source(SpotSource.CREW)
                .address("서울 성동구")
                .latitude(37.5450)
                .longitude(127.0560)
                .area("성수")
                .tags(new ArrayList<>())
                .media(new ArrayList<>())
                .creatorType("crew")
                .isActive(true)
                .likesCount(0).savesCount(0).viewsCount(0)
                .build();

        spot3 = Spot.builder()
                .id(UUID.randomUUID())
                .slug("seongsu-bar")
                .title("성수 바")
                .category(SpotCategory.BAR)
                .source(SpotSource.CREW)
                .address("서울 성동구")
                .latitude(37.5455)
                .longitude(127.0570)
                .area("성수")
                .tags(new ArrayList<>())
                .media(new ArrayList<>())
                .creatorType("crew")
                .isActive(true)
                .likesCount(0).savesCount(0).viewsCount(0)
                .build();
    }

    @Test
    @DisplayName("Route 생성 시 Spot 참조 및 순서 설정")
    void create_setsSpotReferencesAndOrder() {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setTitle("성수 데이트 코스");
        request.setTheme(RouteTheme.DATE);
        request.setArea("성수");
        request.setCreatorName("Crew");

        CreateRouteRequest.RouteSpotRequest sr1 = new CreateRouteRequest.RouteSpotRequest();
        sr1.setSpotId(spot1.getId());
        sr1.setOrder(1);
        sr1.setStayDuration(60);
        sr1.setWalkingTimeToNext(10);
        sr1.setDistanceToNext(800);
        sr1.setTransitionNote("골목길로 10분");

        CreateRouteRequest.RouteSpotRequest sr2 = new CreateRouteRequest.RouteSpotRequest();
        sr2.setSpotId(spot2.getId());
        sr2.setOrder(2);
        sr2.setStayDuration(90);
        sr2.setWalkingTimeToNext(5);
        sr2.setDistanceToNext(400);

        CreateRouteRequest.RouteSpotRequest sr3 = new CreateRouteRequest.RouteSpotRequest();
        sr3.setSpotId(spot3.getId());
        sr3.setOrder(3);
        sr3.setStayDuration(60);

        request.setSpots(List.of(sr1, sr2, sr3));

        given(routeRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(spot1.getId())).willReturn(Optional.of(spot1));
        given(spotRepository.findById(spot2.getId())).willReturn(Optional.of(spot2));
        given(spotRepository.findById(spot3.getId())).willReturn(Optional.of(spot3));
        given(routeRepository.save(any(Route.class))).willAnswer(inv -> {
            Route r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        Route result = routeService.create(request);

        assertThat(result.getSpots()).hasSize(3);
        assertThat(result.getSpots().get(0).getSpotOrder()).isEqualTo(1);
        assertThat(result.getSpots().get(0).getSpot().getId()).isEqualTo(spot1.getId());
        assertThat(result.getSpots().get(1).getSpotOrder()).isEqualTo(2);
        assertThat(result.getSpots().get(2).getSpotOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("Route 생성 시 totalDuration 자동 계산 (stayDuration + walkingTimeToNext)")
    void create_calculatesTotalDuration() {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setTitle("성수 산책 코스");
        request.setTheme(RouteTheme.WALK);
        request.setArea("성수");
        request.setCreatorName("Crew");

        CreateRouteRequest.RouteSpotRequest sr1 = new CreateRouteRequest.RouteSpotRequest();
        sr1.setSpotId(spot1.getId());
        sr1.setStayDuration(60);
        sr1.setWalkingTimeToNext(10);
        sr1.setDistanceToNext(800);

        CreateRouteRequest.RouteSpotRequest sr2 = new CreateRouteRequest.RouteSpotRequest();
        sr2.setSpotId(spot2.getId());
        sr2.setStayDuration(90);
        sr2.setWalkingTimeToNext(5);
        sr2.setDistanceToNext(400);

        CreateRouteRequest.RouteSpotRequest sr3 = new CreateRouteRequest.RouteSpotRequest();
        sr3.setSpotId(spot3.getId());
        sr3.setStayDuration(45);
        // 마지막 Spot은 walkingTimeToNext 없음

        request.setSpots(List.of(sr1, sr2, sr3));

        given(routeRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(spot1.getId())).willReturn(Optional.of(spot1));
        given(spotRepository.findById(spot2.getId())).willReturn(Optional.of(spot2));
        given(spotRepository.findById(spot3.getId())).willReturn(Optional.of(spot3));
        given(routeRepository.save(any(Route.class))).willAnswer(inv -> {
            Route r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        Route result = routeService.create(request);

        // totalDuration = 60 + 10 + 90 + 5 + 45 = 210
        assertThat(result.getTotalDuration()).isEqualTo(210);
        // totalDistance = 800 + 400 = 1200
        assertThat(result.getTotalDistance()).isEqualTo(1200);
    }

    @Test
    @DisplayName("Route 생성 시 존재하지 않는 Spot ID면 ResourceNotFoundException")
    void create_invalidSpotId_throwsException() {
        UUID invalidId = UUID.randomUUID();
        CreateRouteRequest request = new CreateRouteRequest();
        request.setTitle("테스트 코스");
        request.setTheme(RouteTheme.HANGOUT);
        request.setArea("성수");

        CreateRouteRequest.RouteSpotRequest sr = new CreateRouteRequest.RouteSpotRequest();
        sr.setSpotId(invalidId);
        request.setSpots(List.of(sr));

        given(routeRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(invalidId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spot");
    }

    @Test
    @DisplayName("slug로 Route 상세 조회")
    void getDetailBySlug_returnsRouteDetail() {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .slug("seongsu-date")
                .title("성수 데이트 코스")
                .theme(RouteTheme.DATE)
                .area("성수")
                .totalDuration(210)
                .totalDistance(1200)
                .creatorType("crew")
                .creatorName("Crew")
                .spots(new ArrayList<>())
                .variations(new ArrayList<>())
                .likesCount(10)
                .savesCount(5)
                .replicationsCount(3)
                .completionsCount(2)
                .isActive(true)
                .build();

        given(routeRepository.findBySlugAndIsActiveTrue("seongsu-date"))
                .willReturn(Optional.of(route));

        RouteDetailResponse result = routeService.getDetailBySlug("seongsu-date");

        assertThat(result.getSlug()).isEqualTo("seongsu-date");
        assertThat(result.getTotalDuration()).isEqualTo(210);
        assertThat(result.getTotalDistance()).isEqualTo(1200);
        assertThat(result.getTheme()).isEqualTo(RouteTheme.DATE);
    }

    @Test
    @DisplayName("존재하지 않는 Route slug 조회 시 ResourceNotFoundException")
    void getBySlug_notFound_throwsException() {
        given(routeRepository.findBySlugAndIsActiveTrue("invalid"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getBySlug("invalid"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Route");
    }
}
