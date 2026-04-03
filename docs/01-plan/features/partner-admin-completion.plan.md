# Partner Admin Completion Planning Document

> **Summary**: 파트너 관리 시스템의 미완성 기능 보완 — Admin 수정 페이지, Backend Analytics dailyTrend, QR 코드 삭제 API
>
> **Project**: Spotline Admin + Backend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | Admin에서 파트너 정보 수정 페이지 미구현, Analytics dailyTrend 미반환, QR 코드 개별 삭제 불가 |
| **Solution** | Partner Edit 페이지 + Backend dailyTrend API 보완 + QR 코드 DELETE 엔드포인트 추가 |
| **Function/UX Effect** | 파트너 관리 워크플로우 완성 — 등록→조회→수정→분석의 전체 CRUD 사이클 가동 |
| **Core Value** | QR 파트너 시스템 운영 준비 완료 — 첫 파트너 온보딩 시 admin 도구가 완전 기능 |

---

## 1. Overview

### 1.1 Purpose

기존 파트너 시스템(Phase 8)은 Backend API와 Admin UI 모두 핵심 CRUD가 구현되어 있으나, 몇 가지 미완성 기능이 남아있다. 이 feature는 파트너 관리의 완성도를 높여 실제 파트너 온보딩이 가능한 상태로 만든다.

### 1.2 Background

- Backend: Partner CRUD API 완성, PartnerService/Controller 테스트 완료
- Admin: 파트너 목록, 등록, 상세(정보/QR/분석 탭) 구현 완료
- **미완성**: Edit 페이지 라우트/컴포넌트 없음, Analytics dailyTrend 미반환, QR 삭제 없음

### 1.3 Related Documents

- `springboot-spotLine-backend/docs/archive/2026-04/qr-partner-system-backend/` — 기존 파트너 PDCA
- `admin-spotLine/src/pages/PartnerDetail.tsx` — 수정 버튼이 `/partners/:id/edit`로 이동하지만 해당 라우트 없음
- `admin-spotLine/src/components/PartnerAnalytics.tsx` — dailyTrend 차트 구현되어 있으나 Backend가 데이터 미반환

---

## 2. Scope

### 2.1 In Scope

- [x] Admin: Partner Edit 페이지 (`/partners/:id/edit`) — PartnerForm 재사용
- [x] Admin: App.tsx에 Edit 라우트 등록
- [x] Backend: Analytics API에 dailyTrend 데이터 추가 (일별 스캔 수)
- [x] Backend: QR 코드 개별 삭제 엔드포인트 추가 (`DELETE /api/v2/admin/partners/{id}/qr-codes/{qrCodeId}`)
- [x] Admin: QRCodePreview에 삭제 버튼 추가

### 2.2 Out of Scope

- 파트너 자체 대시보드/포탈 (별도 feature)
- 결제/빌링 연동
- 멀티 매장 파트너 지원 (1:N Spot)
- 계약 만료 자동 알림
- QR 코드 커스텀 디자인

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Repo |
|----|-------------|----------|------|
| FR-01 | Admin에서 기존 파트너 정보 수정 (tier, brandColor, benefitText, contractEndDate, note) | High | admin-spotLine |
| FR-02 | Edit 페이지에서 Spot 선택은 읽기 전용 (변경 불가) | High | admin-spotLine |
| FR-03 | Edit 성공 시 파트너 상세 페이지로 복귀 | Medium | admin-spotLine |
| FR-04 | Backend Analytics API에 dailyTrend 배열 반환 (date, scans) | High | backend |
| FR-05 | dailyTrend 기간은 period 파라미터에 따라 동적 (7d/30d/90d) | Medium | backend |
| FR-06 | QR 코드 개별 삭제 API (soft delete) | Medium | backend |
| FR-07 | Admin QR 코드 프리뷰에 삭제 버튼 추가 | Medium | admin-spotLine |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Performance | dailyTrend 쿼리 < 500ms (90일 기준) |
| UX | Edit 폼은 기존 데이터로 pre-populate |
| Security | 모든 엔드포인트 ROLE_ADMIN 필수 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] Partner Edit 페이지가 `/partners/:id/edit`에서 작동
- [ ] 기존 파트너 데이터로 폼이 pre-populate
- [ ] PATCH API로 수정 저장 + 상세 페이지 복귀
- [ ] Analytics API가 dailyTrend 배열 반환
- [ ] Admin 분석 탭에서 일별 차트가 실제 데이터로 렌더링
- [ ] QR 코드 삭제 API + Admin UI 삭제 버튼 동작
- [ ] Gap Analysis Match Rate >= 90%

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| dailyTrend 쿼리 성능 | Medium | Low | PostgreSQL GROUP BY + 인덱스 활용, period 제한 |
| PartnerForm 재사용 복잡도 | Low | Medium | create/edit 모드 prop으로 분기 |
| QR 삭제 시 스캔 로그 참조 | Medium | Low | soft delete (isActive=false)로 로그 보존 |

---

## 6. Architecture Considerations

### 6.1 Project Level

- **Dynamic** — Spring Boot Backend + React Admin

### 6.2 Key Architectural Decisions

| Decision | Selected | Rationale |
|----------|----------|-----------|
| Edit 폼 구현 방식 | PartnerForm 재사용 + mode prop | 코드 중복 최소화 |
| dailyTrend 쿼리 | Native SQL GROUP BY DATE | JPA 기본 메서드로 부족, @Query 사용 |
| QR 삭제 방식 | Soft delete (isActive=false) | 스캔 로그 FK 무결성 유지 |

### 6.3 변경 대상 파일

**Backend (springboot-spotLine-backend)**:
- `PartnerAnalyticsResponse.java` — dailyTrend 필드 추가
- `QrScanLogRepository.java` — dailyTrend 쿼리 추가
- `PartnerService.java` — getAnalytics에 dailyTrend 조회 로직 추가
- `PartnerController.java` — QR 코드 DELETE 엔드포인트 추가

**Admin (admin-spotLine)**:
- `PartnerEdit.tsx` — NEW: 수정 페이지
- `PartnerForm.tsx` — mode prop 추가 (create/edit), initialData prop
- `App.tsx` — Edit 라우트 등록
- `QRCodePreview.tsx` — 삭제 버튼 추가
- `QRCodeManager.tsx` — 삭제 핸들러 추가
- `partnerAPI.ts` — deleteQRCode 함수 추가
- `v2.ts` (types) — DailyScanTrend 타입 추가

---

## 7. Implementation Order

### Backend (springboot-spotLine-backend)

1. `PartnerAnalyticsResponse` — dailyTrend 필드 + DailyScanTrend 내부 record 추가
2. `QrScanLogRepository` — `findDailyScans(partnerId, since)` @Query 추가
3. `PartnerService.getAnalytics()` — dailyTrend 조회 + 빈 날짜 채우기 로직
4. `PartnerController` — `DELETE /{id}/qr-codes/{qrCodeId}` 엔드포인트 추가
5. `PartnerService` — `deleteQrCode(partnerId, qrCodeId)` 메서드 추가

### Admin (admin-spotLine)

6. `v2.ts` — `DailyScanTrend` 타입, `PartnerAnalyticsResponse.dailyTrend` 필드 확인
7. `partnerAPI.ts` — `deleteQRCode(partnerId, qrCodeId)` 함수 추가
8. `PartnerForm.tsx` — `mode` + `initialData` props 추가, create/edit 분기
9. `PartnerEdit.tsx` — NEW 페이지 (기존 데이터 fetch + PartnerForm 렌더링)
10. `App.tsx` — `/partners/:id/edit` 라우트 등록
11. `QRCodePreview.tsx` — 삭제 버튼 추가
12. `QRCodeManager.tsx` — 삭제 핸들러 + 목록 갱신

---

## 8. Next Steps

1. [ ] Design 문서 작성 (`partner-admin-completion.design.md`)
2. [ ] Backend 구현
3. [ ] Admin 구현
4. [ ] Gap Analysis

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
