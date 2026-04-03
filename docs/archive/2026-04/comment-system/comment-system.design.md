# Comment System Design Document

> **Summary**: Spot/Route 대상 댓글 CRUD + 대댓글(1 depth) 시스템의 백엔드 API 및 프론트엔드 UI 설계
>
> **Project**: Spotline Backend + Frontend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft
> **Planning Doc**: [comment-system.plan.md](../01-plan/features/comment-system.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- 기존 Social 패턴(SpotLike, SocialController)과 일관된 아키텍처
- Polymorphic 댓글 테이블로 Spot/Route 코드 중복 최소화
- 대댓글 1 depth 제한으로 쿼리/UI 복잡도 관리
- Soft delete로 대댓글 무결성 유지

### 1.2 Design Principles

- 기존 Controller → Service → Repository 패턴 준수
- SecurityConfig의 GET permitAll / POST authenticated 룰 활용
- 프론트엔드: SSR 페이지 + 클라이언트 댓글 섹션 (Hydration)

---

## 2. Architecture

### 2.1 Component Diagram

```
┌─────────────────┐     ┌──────────────────────┐     ┌──────────────┐
│  Next.js Front  │────▶│  Spring Boot Backend │────▶│  PostgreSQL  │
│  (CommentSection│     │  (CommentController) │     │  (comments)  │
│   Client Comp)  │     │  (CommentService)    │     │              │
└─────────────────┘     └──────────────────────┘     └──────────────┘
```

### 2.2 Data Flow

```
[댓글 조회 - 비인증 가능]
GET /api/v2/comments?targetType=SPOT&targetId={id}&page=0&size=20
  → CommentController.getComments()
  → CommentService.getComments(targetType, targetId, pageable)
  → CommentRepository.findByTargetTypeAndTargetIdAndParentIsNull(...)
  → 각 댓글의 replies 포함하여 CommentResponse 리스트 반환

[댓글 작성 - 인증 필요]
POST /api/v2/comments  (Authorization: Bearer {jwt})
  → JwtAuthenticationFilter → userId 추출
  → CommentController.createComment(request)
  → CommentService.createComment(userId, request)
    → Comment 저장
    → 대상 Spot/Route commentsCount +1
  → CommentResponse 반환

[댓글 삭제 - 작성자만]
DELETE /api/v2/comments/{id}  (Authorization: Bearer {jwt})
  → CommentService.deleteComment(userId, commentId)
    → userId == comment.userId 검증
    → isDeleted = true (soft delete)
    → 대상 Spot/Route commentsCount -1
```

---

## 3. Data Model

### 3.1 Comment Entity (JPA)

```java
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_target", columnList = "targetType, targetId"),
    @Index(name = "idx_comment_parent", columnList = "parent_id"),
    @Index(name = "idx_comment_user", columnList = "userId"),
    @Index(name = "idx_comment_created", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentTargetType targetType;  // SPOT or ROUTE

    @Column(nullable = false)
    private UUID targetId;  // Spot.id or Route.id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;  // null이면 최상위 댓글

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @Column(nullable = false)
    private String userId;  // Supabase user ID

    @Column(nullable = false)
    private String userName;  // 비정규화

    private String userAvatarUrl;  // nullable

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 3.2 CommentTargetType Enum

```java
public enum CommentTargetType {
    SPOT, ROUTE
}
```

### 3.3 Spot/Route Entity 변경

```java
// Spot.java — 기존 Stats 섹션에 추가
@Builder.Default
private Integer commentsCount = 0;

// Route.java — 기존 Stats 섹션에 추가
@Builder.Default
private Integer commentsCount = 0;
```

### 3.4 Database Schema

```sql
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type VARCHAR(10) NOT NULL,
    target_id UUID NOT NULL,
    parent_id UUID REFERENCES comments(id),
    user_id VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_avatar_url VARCHAR(500),
    content TEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_comment_target ON comments(target_type, target_id);
CREATE INDEX idx_comment_parent ON comments(parent_id);
CREATE INDEX idx_comment_user ON comments(user_id);
CREATE INDEX idx_comment_created ON comments(created_at);

-- Spot/Route 테이블에 컬럼 추가 (JPA auto-ddl로 처리)
ALTER TABLE spots ADD COLUMN comments_count INTEGER DEFAULT 0;
ALTER TABLE routes ADD COLUMN comments_count INTEGER DEFAULT 0;
```

---

## 4. API Specification

### 4.1 Endpoint List

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v2/comments` | 댓글 목록 조회 (페이지네이션) | Public |
| POST | `/api/v2/comments` | 댓글 작성 | Required |
| PUT | `/api/v2/comments/{id}` | 댓글 수정 | Owner only |
| DELETE | `/api/v2/comments/{id}` | 댓글 삭제 (soft) | Owner only |

### 4.2 Detailed Specification

#### `GET /api/v2/comments`

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| targetType | String | Yes | `SPOT` or `ROUTE` |
| targetId | UUID | Yes | 대상 ID |
| page | int | No | 페이지 번호 (default: 0) |
| size | int | No | 페이지 크기 (default: 20, max: 50) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "uuid",
      "targetType": "SPOT",
      "targetId": "uuid",
      "userId": "supabase-uid",
      "userName": "홍길동",
      "userAvatarUrl": "https://...",
      "content": "여기 분위기 정말 좋아요!",
      "isDeleted": false,
      "createdAt": "2026-04-03T12:00:00",
      "updatedAt": "2026-04-03T12:00:00",
      "replies": [
        {
          "id": "uuid",
          "userId": "supabase-uid-2",
          "userName": "김철수",
          "userAvatarUrl": null,
          "content": "맞아요! 저도 좋았어요",
          "isDeleted": false,
          "createdAt": "2026-04-03T13:00:00",
          "updatedAt": "2026-04-03T13:00:00",
          "replies": []
        }
      ]
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

#### `POST /api/v2/comments`

**Request Body:**
```json
{
  "targetType": "SPOT",
  "targetId": "uuid",
  "content": "여기 분위기 정말 좋아요!",
  "parentId": null
}
```

**Validation Rules:**
- `content`: 1~500자, 빈 문자열 불가
- `parentId`: nullable. 제공 시 해당 댓글이 같은 target에 속하는지 검증
- `parentId`가 이미 대댓글인 경우 거부 (1 depth 제한)

**Response (201 Created):**
```json
{
  "id": "uuid",
  "targetType": "SPOT",
  "targetId": "uuid",
  "userId": "supabase-uid",
  "userName": "홍길동",
  "userAvatarUrl": "https://...",
  "content": "여기 분위기 정말 좋아요!",
  "isDeleted": false,
  "createdAt": "2026-04-03T12:00:00",
  "updatedAt": "2026-04-03T12:00:00",
  "replies": []
}
```

**Error Responses:**
- `400`: content 빈값 / parentId가 대댓글 / parentId가 다른 target
- `401`: 인증 필요
- `404`: targetId 또는 parentId에 해당하는 리소스 없음

#### `PUT /api/v2/comments/{id}`

**Request Body:**
```json
{
  "content": "수정된 댓글 내용"
}
```

**Validation:**
- `content`: 1~500자
- 요청자 userId == 댓글 userId 검증
- isDeleted == true인 댓글은 수정 불가

**Response (200 OK):** CommentResponse (수정된 내용)

**Error:**
- `403`: 작성자가 아닌 경우
- `404`: 댓글 없음 또는 삭제됨

#### `DELETE /api/v2/comments/{id}`

**Response (200 OK):**
```json
{
  "id": "uuid",
  "isDeleted": true
}
```

**동작:**
- `isDeleted = true` 설정 (soft delete)
- `content`는 유지 (DB에만, API 응답에서는 "삭제된 댓글입니다"로 대체)
- 대상 Spot/Route의 `commentsCount` -1
- 대댓글이 있어도 부모 댓글은 soft delete (대댓글은 유지)

**Error:**
- `403`: 작성자가 아닌 경우
- `404`: 댓글 없음

---

## 5. UI/UX Design

### 5.1 댓글 섹션 레이아웃 (Spot/Route 공용)

```
┌────────────────────────────────────────┐
│  💬 댓글 (12)                          │
├────────────────────────────────────────┤
│  ┌──────────────────────────────────┐  │
│  │ [Avatar] 홍길동 · 2시간 전       │  │
│  │ 여기 분위기 정말 좋아요!         │  │
│  │ [답글] [···]                     │  │
│  │                                  │  │
│  │   ┌──────────────────────────┐   │  │
│  │   │ [A] 김철수 · 1시간 전    │   │  │
│  │   │ 맞아요! 저도 좋았어요    │   │  │
│  │   │ [···]                    │   │  │
│  │   └──────────────────────────┘   │  │
│  │                                  │  │
│  │   ▼ 답글 2개 더 보기            │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │ [Avatar] 삭제된 댓글입니다       │  │
│  │                                  │  │
│  │   ┌──────────────────────────┐   │  │
│  │   │ [A] 박영희 · 30분 전     │   │  │
│  │   │ 답글 내용...             │   │  │
│  │   └──────────────────────────┘   │  │
│  └──────────────────────────────────┘  │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │ [더 보기] (페이지 2/3)           │  │
│  └──────────────────────────────────┘  │
├────────────────────────────────────────┤
│  ┌──────────────────────────────────┐  │
│  │ 댓글을 작성하세요...      [작성] │  │
│  └──────────────────────────────────┘  │
│  (비로그인 시: "로그인 후 댓글 작성")  │
└────────────────────────────────────────┘
```

### 5.2 User Flow

```
[댓글 읽기 — 비로그인 OK]
페이지 로드 → CommentSection 마운트 → GET /comments 호출 → 목록 렌더링

[댓글 작성]
로그인 상태 확인 → 입력 → 작성 버튼 → POST /comments → Optimistic UI로 목록 업데이트

[대댓글]
"답글" 버튼 클릭 → 인라인 입력창 표시 → POST /comments (parentId 포함) → 대댓글 추가

[댓글 수정]
"···" 메뉴 → "수정" → 입력창으로 전환 → PUT /comments/{id} → 업데이트

[댓글 삭제]
"···" 메뉴 → "삭제" → 확인 → DELETE /comments/{id} → "삭제된 댓글입니다"로 교체
```

### 5.3 Component List

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `CommentSection` | `src/components/comment/CommentSection.tsx` | 댓글 섹션 컨테이너 (목록 + 작성폼), 데이터 페칭, 페이지네이션 |
| `CommentItem` | `src/components/comment/CommentItem.tsx` | 개별 댓글 렌더링 (아바타, 이름, 내용, 시간, 액션) |
| `CommentForm` | `src/components/comment/CommentForm.tsx` | 댓글/대댓글 작성 입력 폼 |
| `CommentMenu` | `src/components/comment/CommentMenu.tsx` | 수정/삭제 드롭다운 메뉴 (작성자에게만 표시) |

---

## 6. Error Handling

### 6.1 Backend Error Codes

| Code | Message | Cause | Handling |
|------|---------|-------|----------|
| 400 | "댓글 내용을 입력해주세요" | content 빈값 | 클라이언트 유효성 검사 |
| 400 | "대댓글에는 답글을 달 수 없습니다" | parentId가 이미 대댓글 | 에러 토스트 |
| 401 | Unauthorized | 토큰 없음/만료 | 로그인 유도 |
| 403 | "본인의 댓글만 수정/삭제할 수 있습니다" | userId 불일치 | 에러 토스트 |
| 404 | "댓글을 찾을 수 없습니다" | 없는 commentId | 에러 토스트 |

### 6.2 Frontend Error Handling

- API 실패 시 토스트 메시지 표시
- Optimistic UI 실패 시 롤백
- 네트워크 에러 시 "다시 시도" 버튼

---

## 7. Security Considerations

- [x] Content XSS 방지: React의 자동 이스케이핑 + 백엔드에서 HTML 태그 제거 불필요 (TEXT 필드로 저장, 렌더링 시 이스케이프)
- [x] 작성자 인증: JWT userId 기반 소유권 검증
- [x] 입력 길이 제한: content 최대 500자
- [x] Rate limiting: 기존 Spring Security 설정 활용
- [x] SQL Injection: Spring Data JPA 파라미터 바인딩으로 방지

---

## 8. Test Plan

### 8.1 Backend Tests

| Type | Target | Tool |
|------|--------|------|
| Unit Test | CommentService | JUnit 5 + Mockito |
| Unit Test | CommentController | @WebMvcTest |

### 8.2 Test Cases

**CommentService Tests:**
- [x] 최상위 댓글 작성 성공 → commentsCount 증가 확인
- [x] 대댓글 작성 성공 → parent 연결 확인
- [x] 대댓글의 대댓글 작성 시도 → 400 에러
- [x] 타인 댓글 수정 시도 → 403 에러
- [x] 타인 댓글 삭제 시도 → 403 에러
- [x] soft delete 후 조회 시 "삭제된 댓글" 표시
- [x] 댓글 삭제 시 commentsCount 감소 확인
- [x] 존재하지 않는 target에 댓글 작성 → 404 에러
- [x] 페이지네이션 동작 확인 (page, size)

**CommentController Tests:**
- [x] GET /comments — 비인증 요청 허용
- [x] POST /comments — 인증 없이 요청 시 401
- [x] POST /comments — 정상 작성 시 201
- [x] PUT /comments/{id} — 작성자 수정 200
- [x] DELETE /comments/{id} — 작성자 삭제 200

---

## 9. Implementation Guide

### 9.1 Backend File Structure (신규 파일)

```
src/main/java/com/spotline/api/
├── domain/
│   ├── entity/
│   │   └── Comment.java                    ← NEW
│   ├── enums/
│   │   └── CommentTargetType.java          ← NEW
│   └── repository/
│       └── CommentRepository.java          ← NEW
├── dto/
│   ├── request/
│   │   ├── CreateCommentRequest.java       ← NEW
│   │   └── UpdateCommentRequest.java       ← NEW
│   └── response/
│       └── CommentResponse.java            ← NEW
├── service/
│   └── CommentService.java                 ← NEW
└── controller/
    └── CommentController.java              ← NEW
```

### 9.2 Backend 수정 파일

```
├── domain/entity/Spot.java                 ← MODIFY (commentsCount 추가)
├── domain/entity/Route.java                ← MODIFY (commentsCount 추가)
├── dto/response/SpotDetailResponse.java    ← MODIFY (commentsCount 필드)
├── dto/response/RouteDetailResponse.java   ← MODIFY (commentsCount 필드)
└── config/SecurityConfig.java              ← MODIFY (GET /comments 허용)
```

### 9.3 Frontend File Structure (신규 파일)

```
src/
├── components/
│   └── comment/
│       ├── CommentSection.tsx              ← NEW
│       ├── CommentItem.tsx                 ← NEW
│       ├── CommentForm.tsx                 ← NEW
│       └── CommentMenu.tsx                 ← NEW
└── lib/
    └── api.ts                              ← MODIFY (댓글 API 함수 추가)
```

### 9.4 Frontend 수정 파일

```
├── app/spot/[slug]/page.tsx                ← MODIFY (CommentSection 추가)
├── app/route/[slug]/page.tsx               ← MODIFY (CommentSection 추가)
└── types/index.ts                          ← MODIFY (Comment 타입 추가)
```

### 9.5 Implementation Order (체크리스트)

#### Backend (springboot-spotLine-backend)

- [ ] 1. `CommentTargetType` Enum 생성 (`domain/enums/`)
- [ ] 2. `Comment` Entity 생성 (`domain/entity/`)
- [ ] 3. `CommentRepository` 생성 (`domain/repository/`)
- [ ] 4. `CreateCommentRequest` DTO 생성 (`dto/request/`)
- [ ] 5. `UpdateCommentRequest` DTO 생성 (`dto/request/`)
- [ ] 6. `CommentResponse` DTO 생성 (`dto/response/`)
- [ ] 7. `CommentService` 구현 (`service/`)
- [ ] 8. `CommentController` 구현 (`controller/`)
- [ ] 9. `SecurityConfig` 업데이트 — `GET /api/v2/comments` permitAll
- [ ] 10. `Spot` Entity에 `commentsCount` 필드 추가
- [ ] 11. `Route` Entity에 `commentsCount` 필드 추가
- [ ] 12. `SpotDetailResponse`에 `commentsCount` 반영
- [ ] 13. `RouteDetailResponse`에 `commentsCount` 반영
- [ ] 14. `CommentServiceTest` 작성 (9 cases)
- [ ] 15. `CommentControllerTest` 작성 (5 cases)

#### Frontend (front-spotLine)

- [ ] 16. `types/index.ts`에 Comment 관련 타입 추가
- [ ] 17. `api.ts`에 댓글 API 함수 4개 추가 (get, create, update, delete)
- [ ] 18. `CommentForm` 컴포넌트 구현
- [ ] 19. `CommentMenu` 컴포넌트 구현
- [ ] 20. `CommentItem` 컴포넌트 구현
- [ ] 21. `CommentSection` 컴포넌트 구현 (목록 + 페이지네이션 + 작성폼)
- [ ] 22. Spot 상세 페이지에 `CommentSection` 통합
- [ ] 23. Route 상세 페이지에 `CommentSection` 통합

---

## 10. Coding Convention Reference

### 10.1 This Feature's Conventions

| Item | Convention Applied |
|------|-------------------|
| Entity 패턴 | `@Builder` + `@Getter/@Setter` + Lombok (SpotLike 패턴 동일) |
| Controller 패턴 | `@RestController` + `@RequiredArgsConstructor` (SocialController 동일) |
| 인증 | `AuthUtil.requireUserId()` / `AuthUtil.getCurrentUserId()` |
| DTO 분리 | Request/Response 별도 클래스 |
| Frontend 컴포넌트 | PascalCase, `"use client"` 디렉티브, `cn()` 유틸리티 |
| API 호출 | `api.ts` 단일 진입점, Axios 인스턴스 사용 |
| 시간 표시 | 상대 시간 ("2시간 전", "3일 전") |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
