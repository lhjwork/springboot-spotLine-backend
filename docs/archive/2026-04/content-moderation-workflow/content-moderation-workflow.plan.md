# Content Moderation Workflow Planning Document

> **Summary**: 사용자 콘텐츠 신고 + Admin 모더레이션 큐 — 댓글/Spot/Route 신고 접수, Admin 검토/처리 워크플로우
>
> **Project**: Spotline Backend + Admin + Frontend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 사용자가 스팸/부적절 댓글을 신고할 수 없고, Admin에 모더레이션 큐가 없어 콘텐츠 관리 불가 |
| **Solution** | ContentReport 엔티티 + 신고 API + Admin 모더레이션 대시보드 + Frontend 신고 버튼 |
| **Function/UX Effect** | 사용자가 부적절 콘텐츠를 1클릭 신고, Admin이 큐에서 검토→숨김/삭제/기각 처리 |
| **Core Value** | 플랫폼 콘텐츠 품질 보장 — 사용자 신뢰 확보 및 서비스 런칭 준비 |

---

## 1. Overview

### 1.1 Purpose

Comment 시스템 구현 완료 후, 사용자 생성 콘텐츠(댓글, 향후 Spot/Route)에 대한 신고 및 모더레이션 워크플로우가 필요하다. 현재는 인증된 사용자만 댓글 작성 가능하지만, 스팸/부적절 콘텐츠를 신고하거나 Admin이 관리할 수단이 없다.

### 1.2 Background

- Comment 시스템: soft delete 구현됨, 신고/모더레이션은 "별도 피처"로 명시적 연기
- Admin 패널: `moderator` 역할이 타입에 정의되어 있으나 실제 사용되지 않음
- Frontend: 댓글에 신고 버튼 없음, 신고 모달 없음
- Backend: ContentReport 엔티티 없음, 모더레이션 API 없음

### 1.3 Related Documents

- `docs/archive/2026-04/comment-system/` — 댓글 시스템 PDCA (Out of Scope: 신고/모더레이션)
- `admin-spotLine/src/types/index.ts` — AdminRole에 "moderator" 정의됨

---

## 2. Scope

### 2.1 In Scope

- [x] Backend: ContentReport 엔티티 + Repository
- [x] Backend: 사용자 신고 API (`POST /api/v2/reports`)
- [x] Backend: Admin 신고 목록 + 처리 API
- [x] Backend: 모더레이션 액션 (댓글 숨김/삭제)
- [x] Admin: 모더레이션 큐 대시보드 페이지
- [x] Admin: 신고 상세 + 처리 UI
- [x] Frontend: 댓글 신고 버튼 + 신고 모달

### 2.2 Out of Scope

- 자동 스팸 필터링 (AI/ML 기반)
- Spot/Route 신고 (댓글 우선, 향후 확장)
- 사용자 계정 정지/밴 기능
- 신고자에게 처리 결과 알림 (notification-system 연동)
- 신고 이의제기/항소 워크플로우
- 콘텐츠 자동 숨김 (N건 이상 신고 시)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Repo |
|----|-------------|----------|------|
| FR-01 | 인증된 사용자가 댓글을 사유와 함께 신고할 수 있다 | High | backend + frontend |
| FR-02 | 같은 사용자가 같은 콘텐츠를 중복 신고할 수 없다 | High | backend |
| FR-03 | Admin이 신고 목록을 상태별로 조회할 수 있다 (PENDING/RESOLVED/DISMISSED) | High | backend + admin |
| FR-04 | Admin이 신고를 검토하고 조치할 수 있다 (댓글 숨김/삭제/기각) | High | backend + admin |
| FR-05 | 모더레이션 조치 시 사유를 기록한다 | Medium | backend + admin |
| FR-06 | Admin 대시보드에 미처리 신고 건수가 표시된다 | Medium | admin |
| FR-07 | 신고 사유: SPAM, INAPPROPRIATE, HARASSMENT, OTHER | High | backend |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Performance | 신고 목록 조회 < 200ms (pagination) |
| Security | 신고 API는 인증 필수, 모더레이션 API는 ROLE_ADMIN 필수 |
| UX | 신고 버튼 클릭 → 모달 → 사유 선택 → 완료 (3단계 이내) |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 사용자가 댓글에서 신고 버튼 클릭 → 사유 선택 → 신고 완료
- [ ] 중복 신고 시 에러 메시지 표시
- [ ] Admin 모더레이션 큐에서 PENDING 신고 목록 확인
- [ ] Admin이 신고를 선택 → 댓글 숨김/삭제 또는 기각 처리
- [ ] 처리된 신고는 RESOLVED/DISMISSED 상태로 변경
- [ ] Gap Analysis Match Rate >= 90%

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 악의적 대량 신고 | Medium | Medium | 사용자당 일일 신고 제한 (10건), 중복 신고 차단 |
| 모더레이터 부재 시 큐 적체 | Medium | High | 대시보드에 미처리 건수 표시, 추후 자동 숨김 규칙 |
| Comment 엔티티 확장 복잡도 | Low | Low | Comment에 moderationStatus 필드 추가만 (soft delete 유지) |

---

## 6. Architecture Considerations

### 6.1 Project Level

- **Dynamic** — Spring Boot Backend + React Admin + Next.js Frontend

### 6.2 Key Architectural Decisions

| Decision | Selected | Rationale |
|----------|----------|-----------|
| 신고 대상 모델링 | targetType + targetId (다형적) | 향후 Spot/Route 신고 확장 용이 |
| 댓글 모더레이션 | Comment.isDeleted 활용 | 기존 soft delete 패턴 재사용 |
| 신고 상태 | ReportStatus enum (PENDING/RESOLVED/DISMISSED) | 단순한 3-state 워크플로우 |
| Admin 권한 | 기존 ROLE_ADMIN 활용 | 별도 moderator 역할은 추후 분리 |

### 6.3 Data Model

```
ContentReport
├── id (UUID, PK)
├── reporterUserId (UUID, FK → User)
├── targetType (Enum: COMMENT)    // 향후 SPOT, ROUTE 확장
├── targetId (UUID)
├── reason (Enum: SPAM, INAPPROPRIATE, HARASSMENT, OTHER)
├── description (String, optional) // 상세 사유
├── status (Enum: PENDING, RESOLVED, DISMISSED)
├── resolvedByAdminId (String, nullable)
├── resolvedAt (LocalDateTime, nullable)
├── moderatorNote (String, nullable)
├── createdAt (LocalDateTime)
└── updatedAt (LocalDateTime)
```

### 6.4 변경 대상 파일

**Backend (springboot-spotLine-backend)**:
- `ContentReport.java` — NEW 엔티티
- `ReportReason.java` — NEW enum
- `ReportStatus.java` — NEW enum
- `ContentReportRepository.java` — NEW repository
- `ContentReportService.java` — NEW 서비스
- `ContentReportController.java` — NEW 컨트롤러 (사용자 신고 + admin 모더레이션)
- `CreateReportRequest.java` — NEW DTO
- `ReportResponse.java` — NEW DTO
- `ResolveReportRequest.java` — NEW DTO
- `CommentService.java` — 모더레이션 삭제 메서드 추가

**Admin (admin-spotLine)**:
- `ModerationQueue.tsx` — NEW 페이지
- `reportAPI.ts` — NEW API 서비스
- `App.tsx` — 라우트 등록
- `Layout.tsx` — 사이드바에 모더레이션 메뉴 추가

**Frontend (front-spotLine)**:
- `ReportModal.tsx` — NEW 신고 모달 컴포넌트
- `CommentMenu.tsx` — 신고 버튼 추가
- `reportAPI.ts` — NEW 신고 API

---

## 7. Implementation Order

### Backend (springboot-spotLine-backend)

1. `ReportReason` + `ReportStatus` enum 생성
2. `ContentReport` 엔티티 생성
3. `ContentReportRepository` 생성
4. `CreateReportRequest` + `ReportResponse` + `ResolveReportRequest` DTO 생성
5. `ContentReportService` 생성 (신고 접수 + 목록 + 처리)
6. `ContentReportController` 생성 (사용자 API + Admin API)
7. `CommentService` — 모더레이션 삭제 메서드 추가

### Admin (admin-spotLine)

8. `reportAPI.ts` — API 서비스 추가
9. `ModerationQueue.tsx` — 모더레이션 큐 페이지
10. `App.tsx` — 라우트 등록
11. `Layout.tsx` — 사이드바 메뉴 추가

### Frontend (front-spotLine)

12. `reportAPI.ts` — 신고 API
13. `ReportModal.tsx` — 신고 모달
14. `CommentMenu.tsx` — 신고 버튼 추가

---

## 8. Next Steps

1. [ ] Design 문서 작성 (`content-moderation-workflow.design.md`)
2. [ ] Backend 구현
3. [ ] Admin 구현
4. [ ] Frontend 구현
5. [ ] Gap Analysis

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
