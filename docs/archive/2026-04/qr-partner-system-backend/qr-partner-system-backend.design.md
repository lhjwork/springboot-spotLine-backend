# QR Partner System Backend — Design Document

> **Feature**: qr-partner-system-backend
> **Plan Reference**: `docs/01-plan/features/qr-partner-system-backend.plan.md`
> **Date**: 2026-04-01
> **Status**: Design

---

## 1. Entity Design

### 1.1 PartnerTier Enum

**File**: `src/main/java/com/spotline/api/domain/enums/PartnerTier.java`

```java
package com.spotline.api.domain.enums;

public enum PartnerTier {
    BASIC,
    PREMIUM
}
```

### 1.2 PartnerStatus Enum

**File**: `src/main/java/com/spotline/api/domain/enums/PartnerStatus.java`

```java
package com.spotline.api.domain.enums;

public enum PartnerStatus {
    ACTIVE,
    PAUSED,
    TERMINATED
}
```

### 1.3 Partner Entity

**File**: `src/main/java/com/spotline/api/domain/entity/Partner.java`

```java
package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partner_spot", columnList = "spot_id", unique = true),
        @Index(name = "idx_partner_status", columnList = "status"),
        @Index(name = "idx_partner_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false, unique = true)
    private Spot spot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerTier tier;

    /** 브랜딩 컬러 (#FF6B35 형식) */
    @Column(length = 7)
    private String brandColor;

    /** 혜택 텍스트 ("QR 스캔 고객 10% 할인") */
    @Column(length = 500)
    private String benefitText;

    @Column(nullable = false)
    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    /** 관리 메모 (Admin 전용) */
    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerQrCode> qrCodes = new ArrayList<>();

    /** 총 스캔 수 (denormalized for list query performance) */
    @Builder.Default
    private Integer totalScans = 0;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 1.4 PartnerQrCode Entity

**File**: `src/main/java/com/spotline/api/domain/entity/PartnerQrCode.java`

```java
package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_qr_codes", indexes = {
        @Index(name = "idx_qr_code_qr_id", columnList = "qrId", unique = true),
        @Index(name = "idx_qr_code_partner", columnList = "partner_id"),
        @Index(name = "idx_qr_code_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /** 고유 QR ID — URL에 사용 (partner-{spotSlug}-{seq}) */
    @Column(nullable = false, unique = true)
    private String qrId;

    /** 라벨 ("정문", "카운터" 등) */
    @Column(nullable = false, length = 100)
    private String label;

    @Builder.Default
    private Boolean isActive = true;

    /** 스캔 수 (denormalized) */
    @Builder.Default
    private Integer scansCount = 0;

    private LocalDateTime lastScannedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.5 QrScanLog Entity

**File**: `src/main/java/com/spotline/api/domain/entity/QrScanLog.java`

```java
package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_scan_logs", indexes = {
        @Index(name = "idx_scan_log_qr_code", columnList = "qr_code_id"),
        @Index(name = "idx_scan_log_scanned_at", columnList = "scannedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id", nullable = false)
    private PartnerQrCode qrCode;

    /** 익명 세션 ID (Front에서 생성한 클라이언트 세션) */
    private String sessionId;

    private String userAgent;
    private String referer;

    @CreationTimestamp
    private LocalDateTime scannedAt;
}
```

### 1.6 User Entity 수정

**File**: `src/main/java/com/spotline/api/domain/entity/User.java` (수정)

**추가 필드:**
```java
/** 역할 — "user" | "admin" */
@Builder.Default
private String role = "user";
```

**변경 사항**: `role` 필드 1개 추가. 기존 필드 변경 없음.

---

## 2. Repository Design

### 2.1 PartnerRepository

**File**: `src/main/java/com/spotline/api/domain/repository/PartnerRepository.java`

```java
package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    /** 상태별 파트너 목록 (활성만) */
    Page<Partner> findByStatusAndIsActiveTrue(PartnerStatus status, Pageable pageable);

    /** 전체 활성 파트너 목록 */
    Page<Partner> findByIsActiveTrue(Pageable pageable);

    /** ID로 활성 파트너 조회 (QR 코드 포함) */
    @Query("SELECT p FROM Partner p LEFT JOIN FETCH p.qrCodes WHERE p.id = :id AND p.isActive = true")
    Optional<Partner> findByIdWithQrCodes(@Param("id") UUID id);

    /** Spot ID로 활성 파트너 조회 */
    @Query("SELECT p FROM Partner p WHERE p.spot.id = :spotId AND p.isActive = true")
    Optional<Partner> findBySpotIdAndIsActiveTrue(@Param("spotId") UUID spotId);

    /** Spot에 이미 파트너가 있는지 확인 */
    boolean existsBySpotIdAndIsActiveTrue(UUID spotId);
}
```

### 2.2 PartnerQrCodeRepository

**File**: `src/main/java/com/spotline/api/domain/repository/PartnerQrCodeRepository.java`

```java
package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.PartnerQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerQrCodeRepository extends JpaRepository<PartnerQrCode, UUID> {

    /** 파트너의 모든 QR 코드 */
    List<PartnerQrCode> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    /** QR ID로 조회 (스캔 시 사용) */
    Optional<PartnerQrCode> findByQrIdAndIsActiveTrue(String qrId);

    /** QR ID 존재 여부 (중복 체크) */
    boolean existsByQrId(String qrId);

    /** 파트너의 QR 코드 수 */
    int countByPartnerIdAndIsActiveTrue(UUID partnerId);
}
```

### 2.3 QrScanLogRepository

**File**: `src/main/java/com/spotline/api/domain/repository/QrScanLogRepository.java`

```java
package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.QrScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface QrScanLogRepository extends JpaRepository<QrScanLog, UUID> {

    /** 특정 파트너의 기간별 총 스캔 수 */
    @Query("SELECT COUNT(l) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId AND l.scannedAt >= :since")
    long countByPartnerIdSince(@Param("partnerId") UUID partnerId, @Param("since") LocalDateTime since);

    /** 특정 파트너의 기간별 유니크 세션 수 */
    @Query("SELECT COUNT(DISTINCT l.sessionId) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId AND l.scannedAt >= :since AND l.sessionId IS NOT NULL")
    long countUniqueSessionsByPartnerIdSince(@Param("partnerId") UUID partnerId, @Param("since") LocalDateTime since);

    /** 특정 QR 코드의 총 스캔 수 */
    long countByQrCodeId(UUID qrCodeId);
}
```

---

## 3. DTO Design

### 3.1 CreatePartnerRequest

**File**: `src/main/java/com/spotline/api/dto/request/CreatePartnerRequest.java`

```java
package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.PartnerTier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePartnerRequest {

    @NotNull(message = "spotId는 필수입니다")
    private UUID spotId;

    @NotNull(message = "tier는 필수입니다")
    private PartnerTier tier;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "brandColor는 #RRGGBB 형식이어야 합니다")
    private String brandColor;

    private String benefitText;

    @NotNull(message = "contractStartDate는 필수입니다")
    private LocalDate contractStartDate;

    private LocalDate contractEndDate;
    private String note;
}
```

### 3.2 UpdatePartnerRequest

**File**: `src/main/java/com/spotline/api/dto/request/UpdatePartnerRequest.java`

```java
package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePartnerRequest {

    private PartnerStatus status;
    private PartnerTier tier;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "brandColor는 #RRGGBB 형식이어야 합니다")
    private String brandColor;

    private String benefitText;
    private LocalDate contractEndDate;
    private String note;
}
```

### 3.3 CreateQrCodeRequest

**File**: `src/main/java/com/spotline/api/dto/request/CreateQrCodeRequest.java`

```java
package com.spotline.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQrCodeRequest {

    @NotBlank(message = "label은 필수입니다")
    private String label;
}
```

### 3.4 PartnerResponse

**File**: `src/main/java/com/spotline/api/dto/response/PartnerResponse.java`

```java
package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PartnerResponse {
    private UUID id;
    private UUID spotId;
    private String spotSlug;
    private String spotTitle;
    private PartnerStatus status;
    private PartnerTier tier;
    private String brandColor;
    private String benefitText;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String note;
    private Integer totalScans;
    private Integer qrCodeCount;
    private List<PartnerQrCodeResponse> qrCodes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PartnerResponse from(Partner partner) {
        return from(partner, false);
    }

    public static PartnerResponse from(Partner partner, boolean includeQrCodes) {
        List<PartnerQrCodeResponse> qrCodeResponses = null;
        if (includeQrCodes && partner.getQrCodes() != null) {
            qrCodeResponses = partner.getQrCodes().stream()
                    .map(PartnerQrCodeResponse::from)
                    .toList();
        }

        return PartnerResponse.builder()
                .id(partner.getId())
                .spotId(partner.getSpot().getId())
                .spotSlug(partner.getSpot().getSlug())
                .spotTitle(partner.getSpot().getTitle())
                .status(partner.getStatus())
                .tier(partner.getTier())
                .brandColor(partner.getBrandColor())
                .benefitText(partner.getBenefitText())
                .contractStartDate(partner.getContractStartDate())
                .contractEndDate(partner.getContractEndDate())
                .note(partner.getNote())
                .totalScans(partner.getTotalScans())
                .qrCodeCount(partner.getQrCodes() != null ? partner.getQrCodes().size() : 0)
                .qrCodes(qrCodeResponses)
                .isActive(partner.getIsActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
```

### 3.5 PartnerQrCodeResponse

**File**: `src/main/java/com/spotline/api/dto/response/PartnerQrCodeResponse.java`

```java
package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.PartnerQrCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PartnerQrCodeResponse {
    private UUID id;
    private String qrId;
    private String label;
    private Boolean isActive;
    private Integer scansCount;
    private LocalDateTime lastScannedAt;
    private LocalDateTime createdAt;

    public static PartnerQrCodeResponse from(PartnerQrCode qrCode) {
        return PartnerQrCodeResponse.builder()
                .id(qrCode.getId())
                .qrId(qrCode.getQrId())
                .label(qrCode.getLabel())
                .isActive(qrCode.getIsActive())
                .scansCount(qrCode.getScansCount())
                .lastScannedAt(qrCode.getLastScannedAt())
                .createdAt(qrCode.getCreatedAt())
                .build();
    }
}
```

### 3.6 PartnerAnalyticsResponse

**File**: `src/main/java/com/spotline/api/dto/response/PartnerAnalyticsResponse.java`

```java
package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PartnerAnalyticsResponse {
    private UUID partnerId;
    private String spotTitle;
    private String period;           // "7d", "30d", "90d"
    private long totalScans;
    private long uniqueVisitors;
    private double conversionRate;   // uniqueVisitors / totalScans
}
```

### 3.7 SpotPartnerInfo (Spot API 확장용)

**File**: `src/main/java/com/spotline/api/dto/response/SpotPartnerInfo.java`

```java
package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SpotPartnerInfo {
    private boolean isPartner;
    private String brandColor;
    private String benefitText;
    private PartnerTier tier;
    private LocalDate partnerSince;

    public static SpotPartnerInfo from(Partner partner) {
        return SpotPartnerInfo.builder()
                .isPartner(true)
                .brandColor(partner.getBrandColor())
                .benefitText(partner.getBenefitText())
                .tier(partner.getTier())
                .partnerSince(partner.getContractStartDate())
                .build();
    }
}
```

---

## 4. Service Design

### 4.1 PartnerService

**File**: `src/main/java/com/spotline/api/service/PartnerService.java`

```java
package com.spotline.api.service;

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
        // 1. Spot 존재 확인
        Spot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Spot", request.getSpotId().toString()));

        // 2. 이미 파트너인지 확인 (1 Spot = 1 Partner)
        if (partnerRepository.existsBySpotIdAndIsActiveTrue(spot.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 파트너로 등록된 Spot입니다");
        }

        // 3. Partner 생성
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
        // QR 코드도 비활성화
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
        // 파트너 존재 확인
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

    // ---- QR Scan ----

    /** QR 스캔 로그 기록 + 카운트 증가 */
    @Transactional
    public void recordScan(String qrId, String sessionId, String userAgent, String referer) {
        PartnerQrCode qrCode = qrCodeRepository.findByQrIdAndIsActiveTrue(qrId)
                .orElse(null);

        if (qrCode == null) {
            log.debug("QR 코드를 찾을 수 없음: {}", qrId);
            return; // 파트너 QR이 아닌 경우 무시 (graceful)
        }

        QrScanLog scanLog = QrScanLog.builder()
                .qrCode(qrCode)
                .sessionId(sessionId)
                .userAgent(userAgent)
                .referer(referer)
                .build();
        scanLogRepository.save(scanLog);

        // denormalized count 증가
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

        return PartnerAnalyticsResponse.builder()
                .partnerId(partnerId)
                .spotTitle(partner.getSpot().getTitle())
                .period(period != null ? period : "30d")
                .totalScans(totalScans)
                .uniqueVisitors(uniqueVisitors)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
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

    /** QR ID 생성: partner-{spotSlug}-{seq} */
    private String generateQrId(String spotSlug, UUID partnerId) {
        int count = qrCodeRepository.countByPartnerIdAndIsActiveTrue(partnerId) + 1;
        String qrId = "partner-" + spotSlug + "-" + String.format("%03d", count);

        // 중복 체크
        while (qrCodeRepository.existsByQrId(qrId)) {
            count++;
            qrId = "partner-" + spotSlug + "-" + String.format("%03d", count);
        }
        return qrId;
    }

    /** 기간 문자열 → LocalDateTime 변환 */
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
```

---

## 5. Controller Design

### 5.1 PartnerController

**File**: `src/main/java/com/spotline/api/controller/PartnerController.java`

```java
package com.spotline.api.controller;

@RestController
@RequestMapping("/api/v2/admin/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    // ---- Partner CRUD ----

    @PostMapping
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<PartnerResponse>> list(
            @RequestParam(required = false) PartnerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(partnerService.list(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePartnerRequest request) {
        return ResponseEntity.ok(partnerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- QR Code Management ----

    @PostMapping("/{id}/qr-codes")
    public ResponseEntity<PartnerQrCodeResponse> createQrCode(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQrCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.createQrCode(id, request));
    }

    @GetMapping("/{id}/qr-codes")
    public ResponseEntity<List<PartnerQrCodeResponse>> listQrCodes(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.listQrCodes(id));
    }

    @PatchMapping("/{id}/qr-codes/{qrCodeId}")
    public ResponseEntity<PartnerQrCodeResponse> updateQrCode(
            @PathVariable UUID id,
            @PathVariable UUID qrCodeId,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(partnerService.updateQrCode(id, qrCodeId, isActive));
    }

    // ---- Analytics ----

    @GetMapping("/{id}/analytics")
    public ResponseEntity<PartnerAnalyticsResponse> getAnalytics(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(partnerService.getAnalytics(id, period));
    }
}
```

### 5.2 QrScanController (Public)

**File**: `src/main/java/com/spotline/api/controller/QrScanController.java`

```java
package com.spotline.api.controller;

@RestController
@RequestMapping("/api/v2/qr")
@RequiredArgsConstructor
public class QrScanController {

    private final PartnerService partnerService;

    /** QR 스캔 로그 기록 — fire-and-forget, 항상 200 반환 */
    @PostMapping("/{qrId}/scan")
    public ResponseEntity<Void> recordScan(
            @PathVariable String qrId,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer) {
        partnerService.recordScan(qrId, sessionId, userAgent, referer);
        return ResponseEntity.ok().build();
    }
}
```

---

## 6. Security Config 수정

### 6.1 SecurityConfig 변경

**File**: `src/main/java/com/spotline/api/config/SecurityConfig.java` (수정)

**변경 사항**: Admin 경로에 역할 기반 접근 제어 추가.

```java
// 기존 authorizeHttpRequests 블록에 추가 (인증 필요 — 쓰기 작업 위에 삽입):

// Admin 전용 — ROLE_ADMIN 필요
.requestMatchers("/api/v2/admin/**").hasRole("ADMIN")

// Public — QR 스캔 로그 (인증 불필요)
.requestMatchers(HttpMethod.POST, "/api/v2/qr/*/scan").permitAll()
```

**전체 순서**:
```java
.authorizeHttpRequests(auth -> auth
    // Public — 읽기 전용
    .requestMatchers(HttpMethod.GET, "/api/v2/spots/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v2/routes/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v2/places/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v2/users/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/health").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    // Public — QR 스캔 로그 (POST이지만 인증 불필요)
    .requestMatchers(HttpMethod.POST, "/api/v2/qr/*/scan").permitAll()
    // Admin 전용 — ROLE_ADMIN 필요
    .requestMatchers("/api/v2/admin/**").hasRole("ADMIN")
    // 인증 필요 — 쓰기 작업
    .requestMatchers(HttpMethod.POST, "/api/v2/**").authenticated()
    .requestMatchers(HttpMethod.PUT, "/api/v2/**").authenticated()
    .requestMatchers(HttpMethod.DELETE, "/api/v2/**").authenticated()
    .requestMatchers(HttpMethod.PATCH, "/api/v2/**").authenticated()
    // 나머지 GET은 허용
    .anyRequest().permitAll()
)
```

**핵심**: JwtAuthenticationFilter가 이미 JWT에서 role을 추출하여 `ROLE_ADMIN` authority를 설정하므로, SecurityConfig에서 `.hasRole("ADMIN")`만 추가하면 동작한다.

### 6.2 AuthUtil 수정

**File**: `src/main/java/com/spotline/api/security/AuthUtil.java` (수정)

**추가 메서드:**
```java
/** 현재 사용자의 역할 확인 */
public boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getAuthorities() != null) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    return false;
}
```

---

## 7. SpotDetailResponse 확장

### 7.1 SpotDetailResponse 수정

**File**: `src/main/java/com/spotline/api/dto/response/SpotDetailResponse.java` (수정)

**추가 필드:**
```java
/** 파트너 정보 (파트너 매장인 경우에만, 아니면 null) */
private SpotPartnerInfo partner;
```

**from() 메서드 확장 — 4번째 오버로드 추가:**
```java
public static SpotDetailResponse from(Spot spot, PlaceInfo placeInfo, String s3BaseUrl, SpotPartnerInfo partnerInfo) {
    // 기존 from(spot, placeInfo, s3BaseUrl) 로직 동일
    // + .partner(partnerInfo) 추가
}
```

### 7.2 SpotService 수정

**File**: `src/main/java/com/spotline/api/service/SpotService.java` (수정)

**변경**: `getBySlug()` 메서드에서 파트너 정보 병합.

```java
// 기존 의존성에 추가
private final PartnerService partnerService;

// getBySlug() 수정
public SpotDetailResponse getBySlug(String slug) {
    Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Spot", slug));

    PlaceInfo placeInfo = resolvePlaceInfo(spot);
    SpotPartnerInfo partnerInfo = partnerService.getPartnerInfoBySpotId(spot.getId());
    return SpotDetailResponse.from(spot, placeInfo, getS3BaseUrl(), partnerInfo);
}
```

---

## 8. Implementation Order

| Step | Phase | Files | Description | FR |
|------|-------|-------|-------------|-----|
| **A1** | Enums | `PartnerTier.java` | Partner 등급 열거형 | FR-16 |
| **A2** | Enums | `PartnerStatus.java` | Partner 상태 열거형 | FR-16 |
| **A3** | Entity | `Partner.java` | Partner 엔티티 | FR-01 |
| **A4** | Entity | `PartnerQrCode.java` | QR 코드 엔티티 | FR-02 |
| **A5** | Entity | `QrScanLog.java` | 스캔 로그 엔티티 | FR-13 |
| **A6** | Entity | `User.java` 수정 — role 필드 추가 | Admin 역할 지원 | FR-14 |
| **B1** | Repository | `PartnerRepository.java` | Partner 조회 쿼리 | FR-03~07 |
| **B2** | Repository | `PartnerQrCodeRepository.java` | QR 코드 조회 쿼리 | FR-08~10 |
| **B3** | Repository | `QrScanLogRepository.java` | 스캔 집계 쿼리 | FR-13 |
| **C1** | DTO | `CreatePartnerRequest.java` | 파트너 등록 요청 | FR-03 |
| **C2** | DTO | `UpdatePartnerRequest.java` | 파트너 수정 요청 | FR-06 |
| **C3** | DTO | `CreateQrCodeRequest.java` | QR 코드 생성 요청 | FR-08 |
| **C4** | DTO | `PartnerResponse.java` | 파트너 응답 | FR-04~05 |
| **C5** | DTO | `PartnerQrCodeResponse.java` | QR 코드 응답 | FR-09 |
| **C6** | DTO | `PartnerAnalyticsResponse.java` | 분석 응답 | FR-13 |
| **C7** | DTO | `SpotPartnerInfo.java` | Spot 파트너 정보 | FR-12 |
| **D1** | Service | `PartnerService.java` | 전체 비즈니스 로직 | FR-03~13 |
| **E1** | Controller | `PartnerController.java` | Admin 파트너 API | FR-03~10, FR-13 |
| **E2** | Controller | `QrScanController.java` | QR 스캔 로그 API | FR-11 |
| **E3** | Security | `SecurityConfig.java` 수정 | Admin 경로 + QR scan 경로 | FR-15 |
| **E4** | Security | `AuthUtil.java` 수정 | isAdmin() 메서드 추가 | FR-15 |
| **F1** | Spot 확장 | `SpotDetailResponse.java` 수정 | partner 필드 추가 | FR-12 |
| **F2** | Spot 확장 | `SpotService.java` 수정 | PartnerService 주입 + 파트너 정보 병합 | FR-12 |
| **G1** | Test | `PartnerServiceTest.java` | Service 유닛 테스트 | - |
| **G2** | Test | `PartnerControllerTest.java` | Controller 유닛 테스트 | - |

**총 신규 파일**: 16개 (Enum 2 + Entity 3 + Repository 3 + DTO 7 + Service 1 + Controller 2)
**수정 파일**: 4개 (User.java, SecurityConfig.java, AuthUtil.java, SpotDetailResponse.java, SpotService.java) = 5개

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-01 | Initial design | Development Team |
