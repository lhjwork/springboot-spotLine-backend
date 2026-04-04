package com.spotline.api.service;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.SpotLineTheme;
import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.infrastructure.s3.S3Service;
import com.spotline.api.dto.request.CreateSpotLineRequest;
import com.spotline.api.dto.response.SpotLineDetailResponse;
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
class SpotLineServiceTest {

    @Mock
    private SpotLineRepository spotLineRepository;
    @Mock
    private SpotRepository spotRepository;
    @Mock
    private S3Service s3Service;
    @InjectMocks
    private SpotLineService spotLineService;

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
    @DisplayName("SpotLine 생성 시 Spot 참조 및 순서 설정")
    void create_setsSpotReferencesAndOrder() {
        CreateSpotLineRequest request = new CreateSpotLineRequest();
        request.setTitle("성수 데이트 코스");
        request.setTheme(SpotLineTheme.DATE);
        request.setArea("성수");
        request.setCreatorName("Crew");

        CreateSpotLineRequest.SpotLineSpotRequest sr1 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr1.setSpotId(spot1.getId());
        sr1.setOrder(1);
        sr1.setStayDuration(60);
        sr1.setWalkingTimeToNext(10);
        sr1.setDistanceToNext(800);
        sr1.setTransitionNote("골목길로 10분");

        CreateSpotLineRequest.SpotLineSpotRequest sr2 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr2.setSpotId(spot2.getId());
        sr2.setOrder(2);
        sr2.setStayDuration(90);
        sr2.setWalkingTimeToNext(5);
        sr2.setDistanceToNext(400);

        CreateSpotLineRequest.SpotLineSpotRequest sr3 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr3.setSpotId(spot3.getId());
        sr3.setOrder(3);
        sr3.setStayDuration(60);

        request.setSpots(List.of(sr1, sr2, sr3));

        given(spotLineRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(spot1.getId())).willReturn(Optional.of(spot1));
        given(spotRepository.findById(spot2.getId())).willReturn(Optional.of(spot2));
        given(spotRepository.findById(spot3.getId())).willReturn(Optional.of(spot3));
        given(spotLineRepository.save(any(SpotLine.class))).willAnswer(inv -> {
            SpotLine sl = inv.getArgument(0);
            sl.setId(UUID.randomUUID());
            return sl;
        });

        SpotLine result = spotLineService.create(request, null, "crew");

        assertThat(result.getSpots()).hasSize(3);
        assertThat(result.getSpots().get(0).getSpotOrder()).isEqualTo(1);
        assertThat(result.getSpots().get(0).getSpot().getId()).isEqualTo(spot1.getId());
        assertThat(result.getSpots().get(1).getSpotOrder()).isEqualTo(2);
        assertThat(result.getSpots().get(2).getSpotOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("SpotLine 생성 시 totalDuration 자동 계산 (stayDuration + walkingTimeToNext)")
    void create_calculatesTotalDuration() {
        CreateSpotLineRequest request = new CreateSpotLineRequest();
        request.setTitle("성수 산책 코스");
        request.setTheme(SpotLineTheme.WALK);
        request.setArea("성수");
        request.setCreatorName("Crew");

        CreateSpotLineRequest.SpotLineSpotRequest sr1 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr1.setSpotId(spot1.getId());
        sr1.setStayDuration(60);
        sr1.setWalkingTimeToNext(10);
        sr1.setDistanceToNext(800);

        CreateSpotLineRequest.SpotLineSpotRequest sr2 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr2.setSpotId(spot2.getId());
        sr2.setStayDuration(90);
        sr2.setWalkingTimeToNext(5);
        sr2.setDistanceToNext(400);

        CreateSpotLineRequest.SpotLineSpotRequest sr3 = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr3.setSpotId(spot3.getId());
        sr3.setStayDuration(45);
        // 마지막 Spot은 walkingTimeToNext 없음

        request.setSpots(List.of(sr1, sr2, sr3));

        given(spotLineRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(spot1.getId())).willReturn(Optional.of(spot1));
        given(spotRepository.findById(spot2.getId())).willReturn(Optional.of(spot2));
        given(spotRepository.findById(spot3.getId())).willReturn(Optional.of(spot3));
        given(spotLineRepository.save(any(SpotLine.class))).willAnswer(inv -> {
            SpotLine sl = inv.getArgument(0);
            sl.setId(UUID.randomUUID());
            return sl;
        });

        SpotLine result = spotLineService.create(request, null, "crew");

        // totalDuration = 60 + 10 + 90 + 5 + 45 = 210
        assertThat(result.getTotalDuration()).isEqualTo(210);
        // totalDistance = 800 + 400 = 1200
        assertThat(result.getTotalDistance()).isEqualTo(1200);
    }

    @Test
    @DisplayName("SpotLine 생성 시 존재하지 않는 Spot ID면 ResourceNotFoundException")
    void create_invalidSpotId_throwsException() {
        UUID invalidId = UUID.randomUUID();
        CreateSpotLineRequest request = new CreateSpotLineRequest();
        request.setTitle("테스트 코스");
        request.setTheme(SpotLineTheme.HANGOUT);
        request.setArea("성수");

        CreateSpotLineRequest.SpotLineSpotRequest sr = new CreateSpotLineRequest.SpotLineSpotRequest();
        sr.setSpotId(invalidId);
        request.setSpots(List.of(sr));

        given(spotLineRepository.existsBySlug(anyString())).willReturn(false);
        given(spotRepository.findById(invalidId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> spotLineService.create(request, null, "crew"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spot");
    }

    @Test
    @DisplayName("slug로 SpotLine 상세 조회")
    void getDetailBySlug_returnsSpotLineDetail() {
        SpotLine spotLine = SpotLine.builder()
                .id(UUID.randomUUID())
                .slug("seongsu-date")
                .title("성수 데이트 코스")
                .theme(SpotLineTheme.DATE)
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

        given(spotLineRepository.findBySlugAndIsActiveTrue("seongsu-date"))
                .willReturn(Optional.of(spotLine));

        SpotLineDetailResponse result = spotLineService.getDetailBySlug("seongsu-date");

        assertThat(result.getSlug()).isEqualTo("seongsu-date");
        assertThat(result.getTotalDuration()).isEqualTo(210);
        assertThat(result.getTotalDistance()).isEqualTo(1200);
        assertThat(result.getTheme()).isEqualTo(SpotLineTheme.DATE);
    }

    @Test
    @DisplayName("존재하지 않는 SpotLine slug 조회 시 ResourceNotFoundException")
    void getBySlug_notFound_throwsException() {
        given(spotLineRepository.findBySlugAndIsActiveTrue("invalid"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> spotLineService.getBySlug("invalid"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SpotLine");
    }
}
