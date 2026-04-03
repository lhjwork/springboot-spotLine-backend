package com.spotline.api.service;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.entity.PartnerQrCode;
import com.spotline.api.domain.entity.QrScanLog;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.enums.PartnerStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerQrCodeRepository qrCodeRepository;
    private final QrScanLogRepository scanLogRepository;
    private final SpotRepository spotRepository;

    // ---- Partner CRUD ----

    /** 파트너 등록 */
    @Transactional
    public PartnerResponse create(CreatePartnerRequest request) {
        Spot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Spot", request.getSpotId().toString()));

        if (partnerRepository.existsBySpotIdAndIsActiveTrue(spot.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 파트너로 등록된 Spot입니다");
        }

        Partner partner = Partner.builder()
                .spot(spot)
                .status(PartnerStatus.ACTIVE)
                .tier(request.getTier())
                .brandColor(request.getBrandColor())
                .benefitText(request.getBenefitText())
                .contractStartDate(request.getContractStartDate())
                .contractEndDate(request.getContractEndDate())
                .note(request.getNote())
                .build();

        partner = partnerRepository.save(partner);
        return PartnerResponse.from(partner);
    }

    /** 파트너 목록 조회 */
    public Page<PartnerResponse> list(PartnerStatus status, Pageable pageable) {
        Page<Partner> partners;
        if (status != null) {
            partners = partnerRepository.findByStatusAndIsActiveTrue(status, pageable);
        } else {
            partners = partnerRepository.findByIsActiveTrue(pageable);
        }
        return partners.map(PartnerResponse::from);
    }

    /** 파트너 상세 조회 (QR 코드 포함) */
    public PartnerResponse getById(UUID id) {
        Partner partner = partnerRepository.findByIdWithQrCodes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", id.toString()));
        return PartnerResponse.from(partner, true);
    }

    /** 파트너 정보 수정 */
    @Transactional
    public PartnerResponse update(UUID id, UpdatePartnerRequest request) {
        Partner partner = partnerRepository.findByIdWithQrCodes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", id.toString()));

        if (request.getStatus() != null) partner.setStatus(request.getStatus());
        if (request.getTier() != null) partner.setTier(request.getTier());
        if (request.getBrandColor() != null) partner.setBrandColor(request.getBrandColor());
        if (request.getBenefitText() != null) partner.setBenefitText(request.getBenefitText());
        if (request.getContractEndDate() != null) partner.setContractEndDate(request.getContractEndDate());
        if (request.getNote() != null) partner.setNote(request.getNote());

        return PartnerResponse.from(partner, true);
    }

    /** 파트너 해지 (soft delete) */
    @Transactional
    public void delete(UUID id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", id.toString()));
        partner.setIsActive(false);
        partner.setStatus(PartnerStatus.TERMINATED);
        partner.getQrCodes().forEach(qr -> qr.setIsActive(false));
    }

    // ---- QR Code Management ----

    /** QR 코드 생성 */
    @Transactional
    public PartnerQrCodeResponse createQrCode(UUID partnerId, CreateQrCodeRequest request) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", partnerId.toString()));

        String qrId = generateQrId(partner.getSpot().getSlug(), partnerId);

        PartnerQrCode qrCode = PartnerQrCode.builder()
                .partner(partner)
                .qrId(qrId)
                .label(request.getLabel())
                .build();

        qrCode = qrCodeRepository.save(qrCode);
        return PartnerQrCodeResponse.from(qrCode);
    }

    /** 파트너의 QR 코드 목록 */
    public List<PartnerQrCodeResponse> listQrCodes(UUID partnerId) {
        if (!partnerRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Partner", partnerId.toString());
        }
        return qrCodeRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(PartnerQrCodeResponse::from)
                .toList();
    }

    /** QR 코드 비활성화 */
    @Transactional
    public PartnerQrCodeResponse updateQrCode(UUID partnerId, UUID qrCodeId, boolean isActive) {
        PartnerQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("QrCode", qrCodeId.toString()));

        if (!qrCode.getPartner().getId().equals(partnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 파트너의 QR 코드가 아닙니다");
        }

        qrCode.setIsActive(isActive);
        return PartnerQrCodeResponse.from(qrCode);
    }

    /** QR 코드 삭제 (soft delete) */
    @Transactional
    public void deleteQrCode(UUID partnerId, UUID qrCodeId) {
        PartnerQrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("QrCode", qrCodeId.toString()));

        if (!qrCode.getPartner().getId().equals(partnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 파트너의 QR 코드가 아닙니다");
        }

        qrCode.setIsActive(false);
    }

    // ---- QR Scan ----

    /** QR 스캔 로그 기록 + 카운트 증가 */
    @Transactional
    public void recordScan(String qrId, String sessionId, String userAgent, String referer) {
        PartnerQrCode qrCode = qrCodeRepository.findByQrIdAndIsActiveTrue(qrId)
                .orElse(null);

        if (qrCode == null) {
            log.debug("QR 코드를 찾을 수 없음: {}", qrId);
            return;
        }

        QrScanLog scanLog = QrScanLog.builder()
                .qrCode(qrCode)
                .sessionId(sessionId)
                .userAgent(userAgent)
                .referer(referer)
                .build();
        scanLogRepository.save(scanLog);

        qrCode.setScansCount(qrCode.getScansCount() + 1);
        qrCode.setLastScannedAt(LocalDateTime.now());

        Partner partner = qrCode.getPartner();
        partner.setTotalScans(partner.getTotalScans() + 1);
    }

    // ---- Analytics ----

    /** 파트너 분석 데이터 */
    public PartnerAnalyticsResponse getAnalytics(UUID partnerId, String period) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner", partnerId.toString()));

        LocalDateTime since = parsePeriod(period);
        long totalScans = scanLogRepository.countByPartnerIdSince(partnerId, since);
        long uniqueVisitors = scanLogRepository.countUniqueSessionsByPartnerIdSince(partnerId, since);

        double conversionRate = totalScans > 0 ? (double) uniqueVisitors / totalScans : 0;

        List<Object[]> rawDaily = scanLogRepository.findDailyScansByPartnerId(partnerId, since);
        List<PartnerAnalyticsResponse.DailyScanTrend> dailyTrend = buildDailyTrend(since, rawDaily);

        LocalDateTime lastScanAt = scanLogRepository.findLastScanAtByPartnerId(partnerId);

        return PartnerAnalyticsResponse.builder()
                .partnerId(partnerId)
                .spotTitle(partner.getSpot().getTitle())
                .period(period != null ? period : "30d")
                .totalScans(totalScans)
                .uniqueVisitors(uniqueVisitors)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .lastScanAt(lastScanAt)
                .dailyTrend(dailyTrend)
                .build();
    }

    // ---- Spot API 확장용 ----

    /** Spot ID로 파트너 정보 조회 (SpotService에서 호출) */
    public SpotPartnerInfo getPartnerInfoBySpotId(UUID spotId) {
        return partnerRepository.findBySpotIdAndIsActiveTrue(spotId)
                .filter(p -> p.getStatus() == PartnerStatus.ACTIVE)
                .map(SpotPartnerInfo::from)
                .orElse(null);
    }

    // ---- Private helpers ----

    private String generateQrId(String spotSlug, UUID partnerId) {
        int count = qrCodeRepository.countByPartnerIdAndIsActiveTrue(partnerId) + 1;
        String qrId = "partner-" + spotSlug + "-" + String.format("%03d", count);

        while (qrCodeRepository.existsByQrId(qrId)) {
            count++;
            qrId = "partner-" + spotSlug + "-" + String.format("%03d", count);
        }
        return qrId;
    }

    private List<PartnerAnalyticsResponse.DailyScanTrend> buildDailyTrend(
            LocalDateTime since, List<Object[]> rawDaily) {
        Map<String, Long> dateMap = new LinkedHashMap<>();
        for (Object[] row : rawDaily) {
            String date = row[0].toString().substring(0, 10);
            long count = ((Number) row[1]).longValue();
            dateMap.put(date, count);
        }

        List<PartnerAnalyticsResponse.DailyScanTrend> result = new ArrayList<>();
        LocalDate start = since.toLocalDate();
        LocalDate end = LocalDate.now();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String dateStr = d.toString();
            result.add(new PartnerAnalyticsResponse.DailyScanTrend(
                    dateStr, dateMap.getOrDefault(dateStr, 0L)));
        }
        return result;
    }

    private LocalDateTime parsePeriod(String period) {
        if (period == null) period = "30d";
        return switch (period) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            case "90d" -> LocalDateTime.now().minusDays(90);
            case "1y" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusDays(30);
        };
    }
}
