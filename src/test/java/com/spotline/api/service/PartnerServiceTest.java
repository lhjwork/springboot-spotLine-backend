package com.spotline.api.service;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.entity.PartnerQrCode;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import com.spotline.api.domain.repository.PartnerQrCodeRepository;
import com.spotline.api.domain.repository.PartnerRepository;
import com.spotline.api.domain.repository.QrScanLogRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.CreatePartnerRequest;
import com.spotline.api.dto.request.CreateQrCodeRequest;
import com.spotline.api.dto.request.UpdatePartnerRequest;
import com.spotline.api.dto.response.PartnerAnalyticsResponse;
import com.spotline.api.dto.response.PartnerQrCodeResponse;
import com.spotline.api.dto.response.PartnerResponse;
import com.spotline.api.dto.response.SpotPartnerInfo;
import com.spotline.api.exception.ResourceNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerQrCodeRepository qrCodeRepository;
    @Mock
    private QrScanLogRepository scanLogRepository;
    @Mock
    private SpotRepository spotRepository;
    @InjectMocks
    private PartnerService partnerService;

    private Spot testSpot;
    private Partner testPartner;
    private PartnerQrCode testQrCode;

    @BeforeEach
    void setUp() {
        testSpot = Spot.builder()
                .id(UUID.randomUUID())
                .slug("cafe-onion")
                .title("카페 어니언")
                .build();

        testPartner = Partner.builder()
                .id(UUID.randomUUID())
                .spot(testSpot)
                .status(PartnerStatus.ACTIVE)
                .tier(PartnerTier.BASIC)
                .brandColor("#FF5733")
                .benefitText("음료 10% 할인")
                .contractStartDate(LocalDate.now())
                .contractEndDate(LocalDate.now().plusYears(1))
                .totalScans(0)
                .qrCodes(new ArrayList<>())
                .build();

        testQrCode = PartnerQrCode.builder()
                .id(UUID.randomUUID())
                .partner(testPartner)
                .qrId("partner-cafe-onion-001")
                .label("입구 QR")
                .scansCount(0)
                .build();
    }

    // ---- Partner CRUD ----

    @Test
    @DisplayName("파트너 등록 성공")
    void create_success() {
        CreatePartnerRequest request = new CreatePartnerRequest();
        request.setSpotId(testSpot.getId());
        request.setTier(PartnerTier.BASIC);
        request.setBrandColor("#FF5733");
        request.setBenefitText("음료 10% 할인");
        request.setContractStartDate(LocalDate.now());
        request.setContractEndDate(LocalDate.now().plusYears(1));

        given(spotRepository.findById(testSpot.getId())).willReturn(Optional.of(testSpot));
        given(partnerRepository.existsBySpotIdAndIsActiveTrue(testSpot.getId())).willReturn(false);
        given(partnerRepository.save(any(Partner.class))).willReturn(testPartner);

        PartnerResponse response = partnerService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getSpotTitle()).isEqualTo("카페 어니언");
        assertThat(response.getTier()).isEqualTo(PartnerTier.BASIC);
    }

    @Test
    @DisplayName("이미 파트너인 Spot에 중복 등록 시 CONFLICT")
    void create_duplicateSpot_throwsConflict() {
        CreatePartnerRequest request = new CreatePartnerRequest();
        request.setSpotId(testSpot.getId());
        request.setTier(PartnerTier.BASIC);
        request.setContractStartDate(LocalDate.now());

        given(spotRepository.findById(testSpot.getId())).willReturn(Optional.of(testSpot));
        given(partnerRepository.existsBySpotIdAndIsActiveTrue(testSpot.getId())).willReturn(true);

        assertThatThrownBy(() -> partnerService.create(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("파트너 목록 조회")
    void list_success() {
        Page<Partner> page = new PageImpl<>(List.of(testPartner));
        given(partnerRepository.findByIsActiveTrue(any())).willReturn(page);

        Page<PartnerResponse> result = partnerService.list(null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("파트너 상세 조회")
    void getById_success() {
        given(partnerRepository.findByIdWithQrCodes(testPartner.getId())).willReturn(Optional.of(testPartner));

        PartnerResponse response = partnerService.getById(testPartner.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testPartner.getId());
    }

    @Test
    @DisplayName("존재하지 않는 파트너 조회 시 404")
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        given(partnerRepository.findByIdWithQrCodes(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnerService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("파트너 정보 수정")
    void update_success() {
        UpdatePartnerRequest request = new UpdatePartnerRequest();
        request.setBrandColor("#00FF00");

        given(partnerRepository.findByIdWithQrCodes(testPartner.getId())).willReturn(Optional.of(testPartner));

        PartnerResponse response = partnerService.update(testPartner.getId(), request);

        assertThat(testPartner.getBrandColor()).isEqualTo("#00FF00");
    }

    @Test
    @DisplayName("파트너 해지 (soft delete)")
    void delete_success() {
        given(partnerRepository.findById(testPartner.getId())).willReturn(Optional.of(testPartner));

        partnerService.delete(testPartner.getId());

        assertThat(testPartner.getIsActive()).isFalse();
        assertThat(testPartner.getStatus()).isEqualTo(PartnerStatus.TERMINATED);
    }

    // ---- QR Code ----

    @Test
    @DisplayName("QR 코드 생성")
    void createQrCode_success() {
        CreateQrCodeRequest request = new CreateQrCodeRequest();
        request.setLabel("입구 QR");

        given(partnerRepository.findById(testPartner.getId())).willReturn(Optional.of(testPartner));
        given(qrCodeRepository.countByPartnerIdAndIsActiveTrue(testPartner.getId())).willReturn(0);
        given(qrCodeRepository.existsByQrId(anyString())).willReturn(false);
        given(qrCodeRepository.save(any(PartnerQrCode.class))).willReturn(testQrCode);

        PartnerQrCodeResponse response = partnerService.createQrCode(testPartner.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getQrId()).isEqualTo("partner-cafe-onion-001");
    }

    @Test
    @DisplayName("QR 코드 목록 조회")
    void listQrCodes_success() {
        given(partnerRepository.existsById(testPartner.getId())).willReturn(true);
        given(qrCodeRepository.findByPartnerIdOrderByCreatedAtDesc(testPartner.getId()))
                .willReturn(List.of(testQrCode));

        List<PartnerQrCodeResponse> result = partnerService.listQrCodes(testPartner.getId());

        assertThat(result).hasSize(1);
    }

    // ---- QR Scan ----

    @Test
    @DisplayName("QR 스캔 로그 기록")
    void recordScan_success() {
        given(qrCodeRepository.findByQrIdAndIsActiveTrue("partner-cafe-onion-001"))
                .willReturn(Optional.of(testQrCode));

        partnerService.recordScan("partner-cafe-onion-001", "session-1", "Mozilla/5.0", null);

        verify(scanLogRepository).save(any());
        assertThat(testQrCode.getScansCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 QR 스캔 시 무시")
    void recordScan_unknownQr_ignored() {
        given(qrCodeRepository.findByQrIdAndIsActiveTrue("unknown")).willReturn(Optional.empty());

        partnerService.recordScan("unknown", null, null, null);
        // 예외 없이 정상 종료
    }

    // ---- Analytics ----

    @Test
    @DisplayName("파트너 분석 데이터 조회")
    void getAnalytics_success() {
        given(partnerRepository.findById(testPartner.getId())).willReturn(Optional.of(testPartner));
        given(scanLogRepository.countByPartnerIdSince(eq(testPartner.getId()), any(LocalDateTime.class)))
                .willReturn(100L);
        given(scanLogRepository.countUniqueSessionsByPartnerIdSince(eq(testPartner.getId()), any(LocalDateTime.class)))
                .willReturn(80L);

        PartnerAnalyticsResponse response = partnerService.getAnalytics(testPartner.getId(), "30d");

        assertThat(response.getTotalScans()).isEqualTo(100);
        assertThat(response.getUniqueVisitors()).isEqualTo(80);
        assertThat(response.getConversionRate()).isEqualTo(0.8);
    }

    // ---- Spot Extension ----

    @Test
    @DisplayName("Spot ID로 파트너 정보 조회 — 파트너인 경우")
    void getPartnerInfoBySpotId_found() {
        given(partnerRepository.findBySpotIdAndIsActiveTrue(testSpot.getId()))
                .willReturn(Optional.of(testPartner));

        SpotPartnerInfo info = partnerService.getPartnerInfoBySpotId(testSpot.getId());

        assertThat(info).isNotNull();
        assertThat(info.isPartner()).isTrue();
        assertThat(info.getBrandColor()).isEqualTo("#FF5733");
    }

    @Test
    @DisplayName("Spot ID로 파트너 정보 조회 — 파트너 아닌 경우")
    void getPartnerInfoBySpotId_notFound() {
        given(partnerRepository.findBySpotIdAndIsActiveTrue(testSpot.getId()))
                .willReturn(Optional.empty());

        SpotPartnerInfo info = partnerService.getPartnerInfoBySpotId(testSpot.getId());

        assertThat(info).isNull();
    }
}
