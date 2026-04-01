# QR Partner System Backend — Plan Document

> **Summary**: QR 파트너 매장 관리 Backend API (Partner CRUD, QR 코드 관리, 파트너 분석)
>
> **Project**: spotline-backend (Spring Boot 3.5)
> **Author**: Development Team
> **Date**: 2026-04-01
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | QR 파트너 매장이 0개인 현재 상태에서 파트너 등록/관리/분석을 위한 Backend API가 전혀 없다. Spot 엔티티에 `qrId`, `qrActive` 필드만 존재하고, Partner 엔티티와 QR 코드 관리 체계가 부재하여 수익화 기반을 구축할 수 없다. |
| **Solution** | Partner 엔티티 + PartnerQrCode 엔티티를 신규 생성하고, Admin 전용 Partner CRUD API + QR 코드 관리 API + 파트너 분석 API를 구축한다. 기존 Spot 상세 API를 확장하여 파트너 정보를 포함시킨다. |
| **Function/UX Effect** | Admin에서 파트너 등록 → QR 코드 발급 → 분석 확인까지 완전한 파트너 라이프사이클 관리 가능. Front에서 Spot 상세 조회 시 파트너 배지/혜택 정보 자동 포함. QR 스캔 시 파트너 여부 판별. |
| **Core Value** | 콘텐츠(Spot/Route) → 트래픽 → 파트너 유치 → 수익화 비즈니스 루프의 Backend 기반 완성. QR 스캔 데이터 기반 파트너 가치 지표 제공. |

| Item | Detail |
|------|--------|
| Feature | QR Partner System Backend (Phase 8 Backend) |
| Created | 2026-04-01 |
| Status | Planning |
| Level | Dynamic |
| Depends On | Spot/Route CRUD (완료), Social Features (완료), Feed Discovery API (완료) |
| Cross-Repo Reference | `front-spotLine/docs/01-plan/features/qr-partner-system.plan.md` |

---

## 1. Overview

### 1.1 Purpose

QR 파트너 매장을 등록, 관리, 분석하기 위한 Backend API를 구축한다. Admin(크루)이 매장을 파트너로 등록하고, QR 코드를 발급하며, 스캔 데이터를 기반으로 파트너 가치를 증명할 수 있는 분석 데이터를 제공한다.

### 1.2 Background

- 현재 파트너 매장 0개, 수익화 기반 없음
- Spot 엔티티에 `qrId`, `qrActive` 필드만 존재 (미사용)
- Admin과 Front 모두 Backend API가 있어야 파트너 기능 구현 가능
- Cold Start 전략: 콘텐츠 확보 후 트래픽 → 파트너 영업의 선순환 루프 필요

### 1.3 Related Documents

- Cross-Repo Plan: `front-spotLine/docs/01-plan/features/qr-partner-system.plan.md`
- CLAUDE.md: 프로젝트 Phase 8 정의

---

## 2. Scope

### 2.1 In Scope

- [ ] Partner 엔티티 + PartnerQrCode 엔티티 (JPA)
- [ ] PartnerTier, PartnerStatus 열거형
- [ ] Partner CRUD API (Admin 전용)
- [ ] QR 코드 생성/조회/비활성화 API
- [ ] QR 코드 → Spot 매핑 조회 API (기존 `qrId` 활용)
- [ ] Spot 상세 API 확장 (파트너 정보 포함)
- [ ] 파트너 기본 분석 API (스캔 수, 전환율)
- [ ] Admin 역할 검증 (User 엔티티에 role 필드 추가)

### 2.2 Out of Scope

| Item | Reason |
|------|--------|
| 결제/정산 시스템 | 별도 Phase — 유저 기반 확보 후 |
| 파트너 셀프 서비스 포털 | v1은 Admin이 직접 등록 |
| 쿠폰/할인 코드 발행 | 혜택은 텍스트 표시만 (v1) |
| QR 코드 이미지 생성 (Backend) | Admin 클라이언트에서 qrcode.react로 생성 |
| 실시간 알림 | 별도 Phase |
| 다중 지점 매장 (체인점) | v1은 1 Partner = 1 Spot |

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | Partner 엔티티 생성 (Spot 1:1 연관, 계약정보, 브랜딩) | High | Pending |
| FR-02 | PartnerQrCode 엔티티 생성 (Partner N:1 연관, 라벨, 활성 상태) | High | Pending |
| FR-03 | POST /api/v2/admin/partners — 파트너 등록 | High | Pending |
| FR-04 | GET /api/v2/admin/partners — 파트너 목록 (상태 필터, 페이징) | High | Pending |
| FR-05 | GET /api/v2/admin/partners/{id} — 파트너 상세 | High | Pending |
| FR-06 | PATCH /api/v2/admin/partners/{id} — 파트너 정보 수정 | Medium | Pending |
| FR-07 | DELETE /api/v2/admin/partners/{id} — 파트너 해지 (soft delete) | Medium | Pending |
| FR-08 | POST /api/v2/admin/partners/{id}/qr-codes — QR 코드 생성 | High | Pending |
| FR-09 | GET /api/v2/admin/partners/{id}/qr-codes — QR 코드 목록 | High | Pending |
| FR-10 | PATCH /api/v2/admin/partners/{id}/qr-codes/{qrCodeId} — QR 비활성화 | Medium | Pending |
| FR-11 | GET /api/v2/qr/{qrId}/spot — 기존 유지 + 파트너 정보 포함 응답 | High | Pending |
| FR-12 | GET /api/v2/spots/{slug} — 파트너 정보 optional 포함 확장 | High | Pending |
| FR-13 | GET /api/v2/admin/partners/{id}/analytics — 파트너 분석 (스캔 수, 기간별) | Medium | Pending |
| FR-14 | User 엔티티에 role 필드 추가 (user/admin) | High | Pending |
| FR-15 | Admin 역할 검증 미들웨어 (@AdminOnly 또는 SecurityConfig) | High | Pending |
| FR-16 | PartnerTier(BASIC, PREMIUM), PartnerStatus(ACTIVE, PAUSED, TERMINATED) 열거형 | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | Partner 목록 조회 < 200ms | 로그 기반 측정 |
| Performance | QR resolve → Spot 조회 < 100ms | 캐싱 포함 |
| Security | Admin API는 인증 + admin 역할 검증 필수 | SecurityConfig + 테스트 |
| Data Integrity | Partner 삭제 시 soft delete (isActive=false) | 유닛 테스트 |
| Consistency | 1 Spot = 최대 1 Partner (unique constraint) | DB 제약 조건 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 모든 FR 구현 완료
- [ ] 유닛 테스트 작성 및 통과 (Service, Controller)
- [ ] 빌드 성공 (./gradlew build)
- [ ] Admin API에 대한 역할 기반 접근 제어 동작

### 4.2 Quality Criteria

- [ ] 기존 26개 테스트 깨지지 않음
- [ ] 신규 테스트 최소 10개 이상
- [ ] Zero lint errors
- [ ] Build succeeds

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Admin 역할 시스템이 없어 보안 취약 | High | High | User 엔티티에 role 추가 + SecurityConfig에서 /admin/** 경로 검증 |
| Partner-Spot 1:1 제약으로 유연성 부족 | Medium | Low | v1은 1:1로 시작, 추후 N:M 확장 가능한 설계 |
| QR ID 충돌 | Medium | Low | UUID 기반 + unique constraint |
| 분석 데이터 테이블 설계 복잡 | Medium | Medium | v1은 QrScanLog 단일 테이블로 단순 집계, 추후 고도화 |

---

## 6. Architecture Considerations

### 6.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites, portfolios | |
| **Dynamic** | Feature-based modules, BaaS integration | Web apps with backend, SaaS MVPs | **V** |
| **Enterprise** | Strict layer separation, DI, microservices | High-traffic systems | |

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| Entity 설계 | Spot에 파트너 필드 추가 vs 별도 Partner 엔티티 | 별도 Partner 엔티티 | 관심사 분리, Spot 엔티티 비대화 방지 |
| QR 코드 관리 | Spot.qrId 활용 vs PartnerQrCode 엔티티 | PartnerQrCode 엔티티 | 1 Partner N QR 코드 지원 (정문, 카운터 등) |
| Admin 인증 | @PreAuthorize vs SecurityConfig 경로 | SecurityConfig 경로 | 기존 패턴 유지, /api/v2/admin/** 경로 분리 |
| QR ID 형식 | UUID vs slug-based | slug-based (partner-{spotSlug}-{seq}) | 사람이 읽기 쉬움, 디버깅 용이 |
| 분석 데이터 | 별도 Analytics 서비스 vs 단순 count | QrScanLog 엔티티 + count 쿼리 | v1은 단순하게, 추후 고도화 |
| 파트너 정보 전달 | 별도 API vs Spot API 확장 | Spot API 확장 (optional partner 필드) | Front에서 추가 API 호출 불필요 |

### 6.3 Folder Structure Preview

```
src/main/java/com/spotline/api/
├── domain/
│   ├── entity/
│   │   ├── Partner.java                 ← 신규
│   │   ├── PartnerQrCode.java           ← 신규
│   │   └── QrScanLog.java               ← 신규
│   ├── enums/
│   │   ├── PartnerTier.java             ← 신규
│   │   └── PartnerStatus.java           ← 신규
│   └── repository/
│       ├── PartnerRepository.java       ← 신규
│       ├── PartnerQrCodeRepository.java ← 신규
│       └── QrScanLogRepository.java     ← 신규
├── dto/
│   ├── request/
│   │   ├── CreatePartnerRequest.java    ← 신규
│   │   └── UpdatePartnerRequest.java    ← 신규
│   └── response/
│       ├── PartnerResponse.java         ← 신규
│       ├── PartnerQrCodeResponse.java   ← 신규
│       └── PartnerAnalyticsResponse.java ← 신규
├── service/
│   └── PartnerService.java              ← 신규
├── controller/
│   └── PartnerController.java           ← 신규 (/api/v2/admin/partners)
└── config/
    └── SecurityConfig.java              ← 수정 (admin 경로 추가)
```

---

## 7. Data Model

### 7.1 Partner Entity

```java
@Entity
@Table(name = "partners")
public class Partner {
    UUID id;

    @OneToOne
    Spot spot;                    // 연결된 Spot (unique)

    @Enumerated(EnumType.STRING)
    PartnerStatus status;         // ACTIVE, PAUSED, TERMINATED

    @Enumerated(EnumType.STRING)
    PartnerTier tier;             // BASIC, PREMIUM

    String brandColor;            // 브랜딩 컬러 (#FF6B35)
    String benefitText;           // 혜택 텍스트 ("QR 스캔 고객 10% 할인")
    LocalDate contractStartDate;  // 계약 시작일
    LocalDate contractEndDate;    // 계약 종료일 (nullable)
    String note;                  // 관리 메모

    @OneToMany(mappedBy = "partner")
    List<PartnerQrCode> qrCodes;

    Integer totalScans;           // 총 스캔 수 (denormalized)
    Boolean isActive;             // soft delete
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

### 7.2 PartnerQrCode Entity

```java
@Entity
@Table(name = "partner_qr_codes")
public class PartnerQrCode {
    UUID id;

    @ManyToOne
    Partner partner;

    String qrId;                  // 고유 QR ID (partner-cafe-onion-001)
    String label;                 // 라벨 ("정문", "카운터")
    Boolean isActive;
    Integer scansCount;           // 스캔 수 (denormalized)
    LocalDateTime lastScannedAt;
    LocalDateTime createdAt;
}
```

### 7.3 QrScanLog Entity (분석용)

```java
@Entity
@Table(name = "qr_scan_logs")
public class QrScanLog {
    UUID id;

    @ManyToOne
    PartnerQrCode qrCode;

    String sessionId;             // 익명 세션 ID
    String userAgent;
    String referer;
    LocalDateTime scannedAt;
}
```

### 7.4 User Entity 수정

```java
// 기존 User 엔티티에 추가
@Builder.Default
private String role = "user";     // "user" | "admin"
```

---

## 8. API Endpoints

### 8.1 Admin Partner API (`/api/v2/admin/partners`)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v2/admin/partners` | 파트너 등록 | Admin |
| GET | `/api/v2/admin/partners` | 파트너 목록 (status, page, size) | Admin |
| GET | `/api/v2/admin/partners/{id}` | 파트너 상세 (QR 코드 포함) | Admin |
| PATCH | `/api/v2/admin/partners/{id}` | 파트너 정보 수정 | Admin |
| DELETE | `/api/v2/admin/partners/{id}` | 파트너 해지 (soft delete) | Admin |

### 8.2 Admin QR Code API

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v2/admin/partners/{id}/qr-codes` | QR 코드 생성 | Admin |
| GET | `/api/v2/admin/partners/{id}/qr-codes` | QR 코드 목록 | Admin |
| PATCH | `/api/v2/admin/partners/{id}/qr-codes/{qrCodeId}` | QR 비활성화/수정 | Admin |

### 8.3 Admin Analytics API

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v2/admin/partners/{id}/analytics` | 파트너 분석 (period param) | Admin |

### 8.4 Public API (기존 확장)

| Method | Path | Change | Auth |
|--------|------|--------|------|
| GET | `/api/v2/spots/{slug}` | `partner?: SpotPartnerInfo` 필드 추가 | Public |
| GET | `/api/v2/qr/{qrId}/spot` | 기존 유지, QR → Spot 매핑 | Public |
| POST | `/api/v2/qr/{qrId}/scan` | 신규 — QR 스캔 로그 기록 | Public |

---

## 9. Implementation Order

| Step | Phase | Files | Description |
|------|-------|-------|-------------|
| **A1** | Entity | `PartnerTier.java`, `PartnerStatus.java` | 열거형 생성 |
| **A2** | Entity | `Partner.java` | Partner 엔티티 |
| **A3** | Entity | `PartnerQrCode.java` | QR 코드 엔티티 |
| **A4** | Entity | `QrScanLog.java` | 스캔 로그 엔티티 |
| **A5** | Entity | `User.java` 수정 | role 필드 추가 |
| **B1** | Repository | `PartnerRepository.java` | Partner 리포지토리 |
| **B2** | Repository | `PartnerQrCodeRepository.java` | QR 코드 리포지토리 |
| **B3** | Repository | `QrScanLogRepository.java` | 스캔 로그 리포지토리 |
| **C1** | DTO | `CreatePartnerRequest.java`, `UpdatePartnerRequest.java` | 요청 DTO |
| **C2** | DTO | `PartnerResponse.java`, `PartnerQrCodeResponse.java`, `PartnerAnalyticsResponse.java` | 응답 DTO |
| **D1** | Service | `PartnerService.java` | 파트너 CRUD + QR 관리 + 분석 비즈니스 로직 |
| **E1** | Controller | `PartnerController.java` | Admin 파트너 API |
| **E2** | Config | `SecurityConfig.java` 수정 | `/api/v2/admin/**` admin 역할 검증 추가 |
| **E3** | Config | `AuthUtil.java` 수정 | `requireAdmin()` 메서드 추가 |
| **F1** | Spot 확장 | `SpotDetailResponse.java` 수정 | `partner` 필드 추가 |
| **F2** | Spot 확장 | `SpotService.java` 수정 | 파트너 정보 조회 + 병합 |
| **G1** | QR API | 기존 QR 엔드포인트 확장 또는 신규 | QR 스캔 로그 기록 API |
| **H1** | Test | `PartnerServiceTest.java` | 서비스 유닛 테스트 |
| **H2** | Test | `PartnerControllerTest.java` | 컨트롤러 테스트 |

---

## 10. Convention Prerequisites

### 10.1 Existing Project Conventions

- [x] `CLAUDE.md` has coding conventions section
- [x] Spring Boot 3.5 + JPA + PostgreSQL architecture
- [x] Controller → Service → Repository 패턴
- [x] Entity: `@Builder` + `@Getter/@Setter` (Lombok)
- [x] DTO: Request/Response 분리
- [x] Slug 기반 조회 패턴

### 10.2 Environment Variables Needed

| Variable | Purpose | Scope | To Be Created |
|----------|---------|-------|:-------------:|
| 기존 변수 유지 | DB, Auth, S3, Place API | Server | 이미 존재 |
| (추가 없음) | Partner는 기존 인프라 활용 | - | - |

---

## 11. Next Steps

1. [ ] Design 문서 작성 (`qr-partner-system-backend.design.md`)
2. [ ] 엔티티/DTO 상세 필드 확정
3. [ ] SecurityConfig admin 역할 검증 방식 결정
4. [ ] 구현 시작

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-01 | Initial draft | Development Team |
