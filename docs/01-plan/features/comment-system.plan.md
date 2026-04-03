# Comment System Planning Document

> **Summary**: Spot과 Route에 댓글을 작성하고 관리할 수 있는 소셜 댓글 시스템
>
> **Project**: Spotline Backend + Frontend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 사용자가 Spot/Route에 대한 의견을 남길 방법이 없어, 소셜 플랫폼으로서 사용자 간 소통이 제한됨 |
| **Solution** | Spot/Route 댓글 CRUD API + 대댓글(1depth) 지원 + 프론트엔드 댓글 UI 구현 |
| **Function/UX Effect** | 장소/경험에 대한 실시간 의견 교환으로 콘텐츠 가치 증대, 재방문 유도 |
| **Core Value** | 경험 공유 플랫폼의 핵심 소셜 루프 완성 — 방문→기록→공유→소통 |

---

## 1. Overview

### 1.1 Purpose

사용자가 Spot(장소)과 Route(경험 코스)에 댓글을 작성하여 경험을 공유하고 소통할 수 있는 기능을 구현한다. 소셜 플랫폼의 핵심 소통 채널로서, 좋아요/저장 기능과 함께 사용자 참여를 강화한다.

### 1.2 Background

- 현재 Social Features(Phase 6)에서 좋아요/저장/팔로우는 구현 완료
- 하지만 **댓글** 기능이 없어 사용자 간 직접 소통 불가
- 경험 기반 플랫폼에서 "이 장소 어때요?", "이 코스 추천해요" 같은 대화가 핵심
- 댓글은 SEO에도 긍정적 (UGC = User Generated Content)

### 1.3 Related Documents

- `front-spotLine/docs/01-plan/features/experience-social-platform.plan.md` — 전체 플랫폼 Plan
- Social Features (Phase 6) — 좋아요/저장/팔로우 구현 완료
- `springboot-spotLine-backend/src/main/java/com/spotline/api/controller/SocialController.java` — 기존 소셜 API 참조

---

## 2. Scope

### 2.1 In Scope

- [x] Backend: Comment Entity + Repository (Spot/Route 공용)
- [x] Backend: Comment CRUD API (작성, 수정, 삭제, 조회)
- [x] Backend: 대댓글 지원 (1 depth, parentComment 참조)
- [x] Backend: 댓글 수 카운트 (Spot/Route에 commentsCount 필드)
- [x] Frontend: Spot 상세 페이지 댓글 섹션
- [x] Frontend: Route 상세 페이지 댓글 섹션
- [x] Frontend: 댓글 작성/수정/삭제 UI
- [x] Frontend: 대댓글 표시 + 작성 UI

### 2.2 Out of Scope

- 댓글 알림 (별도 notification-system 피처로 분리)
- 댓글 좋아요 (v2에서 검토)
- 신고/모더레이션 시스템 (별도 피처)
- 댓글 멘션 (@user)
- 댓글에 이미지/미디어 첨부

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 인증된 사용자가 Spot에 댓글 작성 | High | Pending |
| FR-02 | 인증된 사용자가 Route에 댓글 작성 | High | Pending |
| FR-03 | 댓글 작성자가 자신의 댓글 수정 | High | Pending |
| FR-04 | 댓글 작성자가 자신의 댓글 삭제 (soft delete) | High | Pending |
| FR-05 | 대댓글 작성 (1 depth, parentComment 참조) | Medium | Pending |
| FR-06 | Spot/Route 상세 페이지에서 댓글 목록 조회 (최신순, 페이지네이션) | High | Pending |
| FR-07 | 대댓글 접기/펼치기 UI | Medium | Pending |
| FR-08 | Spot/Route commentsCount 필드 반영 | Medium | Pending |
| FR-09 | 비로그인 사용자도 댓글 목록 읽기 가능 | High | Pending |
| FR-10 | 댓글에 작성자 프로필 정보 (이름, 아바타) 표시 | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | 댓글 목록 응답 < 300ms (20건 기준) | API 응답 시간 측정 |
| Security | 작성자 본인만 수정/삭제 가능 | JWT userId 검증 |
| UX | 댓글 작성 후 즉시 목록에 반영 (Optimistic UI) | 프론트엔드 동작 확인 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] Comment Entity + Repository 구현
- [ ] Comment CRUD API 엔드포인트 구현 (6개)
- [ ] Spot/Route commentsCount 반영
- [ ] Frontend 댓글 섹션 UI 구현
- [ ] 대댓글 표시 + 작성 UI 구현
- [ ] Gap Analysis Match Rate >= 90%

### 4.2 Quality Criteria

- [ ] Backend 테스트 작성 (Service + Controller)
- [ ] 빌드 성공 (backend + frontend)

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 스팸 댓글 | Medium | Medium | 인증된 사용자만 작성 가능 + 추후 신고 기능 |
| 대댓글 깊이 복잡도 | Low | Low | 1 depth로 제한하여 단순화 |
| commentsCount 동기화 | Medium | Low | 댓글 작성/삭제 시 JPA로 직접 업데이트 |

---

## 6. Architecture Considerations

### 6.1 Project Level

- **Dynamic** — BaaS(Supabase) + Spring Boot 백엔드 + Next.js 프론트

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| 댓글 테이블 구조 | Spot/Route 분리 vs 공용 | **공용 (Polymorphic)** | targetType + targetId로 Spot/Route 구분, 코드 중복 최소화 |
| 대댓글 구조 | Self-reference vs Closure Table | **Self-reference (parentId)** | 1 depth만 지원하므로 단순 참조로 충분 |
| 페이지네이션 | Offset vs Cursor | **Offset** | 댓글 수가 극단적으로 많지 않을 것으로 예상 |
| 삭제 방식 | Hard vs Soft | **Soft delete** | 대댓글 존재 시 "삭제된 댓글입니다" 표시 필요 |

### 6.3 데이터 모델 (Comment Entity)

```
comments
├── id (UUID, PK)
├── targetType (ENUM: SPOT, ROUTE)
├── targetId (UUID, 대상 Spot/Route ID)
├── parentId (UUID, nullable — 대댓글인 경우 부모 댓글 ID)
├── userId (String, 작성자 Supabase ID)
├── userName (String, 작성자 이름 — 비정규화)
├── userAvatarUrl (String, nullable — 아바타 URL)
├── content (TEXT, 댓글 내용)
├── isDeleted (Boolean, soft delete)
├── createdAt (TIMESTAMP)
└── updatedAt (TIMESTAMP)

Indexes:
├── idx_comment_target (targetType, targetId, isDeleted) — 목록 조회
├── idx_comment_parent (parentId) — 대댓글 조회
└── idx_comment_user (userId) — 사용자별 댓글
```

### 6.4 API Endpoints

```
# 댓글 목록 조회 (공개)
GET /api/v2/comments?targetType=SPOT&targetId={id}&page=0&size=20

# 댓글 작성 (인증 필요)
POST /api/v2/comments
Body: { targetType, targetId, content, parentId? }

# 댓글 수정 (작성자만)
PUT /api/v2/comments/{commentId}
Body: { content }

# 댓글 삭제 (작성자만, soft delete)
DELETE /api/v2/comments/{commentId}

# 댓글 수 조회
GET /api/v2/comments/count?targetType=SPOT&targetId={id}
```

---

## 7. Convention Prerequisites

### 7.1 Existing Project Conventions

- [x] `CLAUDE.md` has coding conventions section
- [x] ESLint configuration
- [x] TypeScript configuration
- [x] Tailwind CSS 4, 모바일 퍼스트

### 7.2 따를 컨벤션

- Entity: `@Builder` + `@Getter/@Setter` (Lombok) — 기존 패턴 준수
- Controller → Service → Repository 흐름
- DTO: Request/Response 분리
- Frontend: 컴포넌트 PascalCase, `cn()` 유틸리티 사용
- 한글 UI 텍스트, 영어 코드

### 7.3 Environment Variables

- 추가 환경변수 없음 (기존 DB, Auth 설정 활용)

---

## 8. Implementation Order (Backend → Frontend)

### Backend (springboot-spotLine-backend)

1. `CommentTargetType` Enum 생성
2. `Comment` Entity 생성
3. `CommentRepository` 생성
4. `CreateCommentRequest`, `UpdateCommentRequest` DTO 생성
5. `CommentResponse` DTO 생성
6. `CommentService` 비즈니스 로직
7. `CommentController` REST 엔드포인트
8. `SecurityConfig` 업데이트 (GET 공개, POST/PUT/DELETE 인증)
9. Spot/Route Entity에 `commentsCount` 필드 추가
10. 테스트 작성

### Frontend (front-spotLine)

11. `api.ts`에 댓글 API 함수 추가
12. `CommentSection` 컴포넌트 (Spot/Route 공용)
13. `CommentItem` 컴포넌트 (개별 댓글 + 대댓글)
14. `CommentForm` 컴포넌트 (작성/수정 폼)
15. `ReplyList` 컴포넌트 (대댓글 목록)
16. Spot 상세 페이지에 CommentSection 통합
17. Route 상세 페이지에 CommentSection 통합

---

## 9. Next Steps

1. [ ] Design 문서 작성 (`comment-system.design.md`)
2. [ ] Backend 구현 시작
3. [ ] Frontend UI 구현
4. [ ] Gap Analysis

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
