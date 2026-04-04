# social-interactions-backend Plan

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | Front에 Like/Save/Follow/Profile UI가 완전 구현되어 있으나, Backend에 소셜 API가 전무하여 모든 소셜 기능이 작동 불가 |
| **Solution** | Spring Boot에 User/Like/Save/Follow 엔티티 + REST API 구현, Supabase Auth 유저를 로컬 User로 자동 동기화 |
| **Function/UX Effect** | Spot/Route 좋아요/저장, 유저 팔로우/언팔로우, 프로필 조회, 내 저장 목록 — 모두 실시간 동작 |
| **Core Value** | Phase 6 Social Features 완성, 사용자 참여 지표 수집 시작, experience-replication/posting 기반 마련 |

---

## 1. 현황 분석

### 1.1 Frontend (front-spotLine) — 이미 구현됨

| 기능 | 상태 | 파일 |
|------|:----:|------|
| Like/Save 토글 (optimistic update) | ✅ | `useSocialStore.ts`, `SpotBottomBar.tsx`, `RouteBottomBar.tsx` |
| Follow/Unfollow | ✅ | `useSocialStore.ts`, `ProfileHeader.tsx` |
| 프로필 페이지 (내/타인) | ✅ | `profile/me/page.tsx`, `profile/[userId]/page.tsx` |
| 팔로워/팔로잉 목록 | ✅ | `FollowListSheet.tsx` |
| 내 저장 목록 | ✅ | `SavesList.tsx`, `saves/page.tsx` |
| Route 복제 | ✅ | `ReplicateRouteSheet.tsx` |
| 소셜 상태 초기화 | ✅ | `SocialHydrator.tsx` |

**Front가 기대하는 API 엔드포인트:**

```
# Like/Save
POST   /api/v2/spots/{id}/like
POST   /api/v2/spots/{id}/save
POST   /api/v2/routes/{id}/like
POST   /api/v2/routes/{id}/save
GET    /api/v2/spots/{id}/social
GET    /api/v2/routes/{id}/social

# Follow
POST   /api/v2/users/{userId}/follow
DELETE /api/v2/users/{userId}/follow
GET    /api/v2/users/{userId}/follow/status
GET    /api/v2/users/{userId}/followers?page&size
GET    /api/v2/users/{userId}/following?page&size

# Profile
GET    /api/v2/users/{userId}/profile
GET    /api/v2/users/{userId}/likes/spots?page&size
GET    /api/v2/users/{userId}/saves/routes?page&size
GET    /api/v2/users/me/saves?type&page
GET    /api/v2/users/me/routes?status&page

# Replication
POST   /api/v2/routes/{spotLineId}/replicate
GET    /api/v2/routes/{spotLineId}/variations?page
```

### 1.2 Backend (springboot-spotLine-backend) — 미구현

| 항목 | 상태 |
|------|:----:|
| Spot.likesCount / savesCount / viewsCount 필드 | ✅ 존재 (DTO에도 노출) |
| Route.likesCount / savesCount / replicationsCount / completionsCount | ✅ 존재 |
| Route.parentRoute / variations (변형 관계) | ✅ 존재 |
| User 엔티티 | ❌ 없음 |
| Like 엔티티 | ❌ 없음 |
| Save 엔티티 | ❌ 없음 |
| Follow 엔티티 | ❌ 없음 |
| UserRoute 엔티티 (복제) | ❌ 없음 |
| Social API 컨트롤러 | ❌ 없음 |
| User API 컨트롤러 | ❌ 없음 |
| JWT에서 userId 추출 → 서비스에 전달 | ❌ 미사용 |

### 1.3 인증 인프라 — 완료됨

- Supabase JWT 검증 (JwtAuthenticationFilter) ✅
- SecurityContext에 userId(sub) 설정 ✅
- POST/PUT/DELETE → `.authenticated()` ✅

---

## 2. 구현 범위

### 2.1 Backend — 엔티티 (신규)

| # | 엔티티 | 설명 |
|---|--------|------|
| E-1 | `User` | Supabase 유저 로컬 동기화 (id=Supabase UUID, email, nickname, avatar, bio) |
| E-2 | `SpotLike` | 유저-Spot 좋아요 관계 (userId + spotId 유니크) |
| E-3 | `SpotSave` | 유저-Spot 저장 관계 |
| E-4 | `RouteLike` | 유저-Route 좋아요 관계 |
| E-5 | `RouteSave` | 유저-Route 저장 관계 |
| E-6 | `UserFollow` | 팔로우 관계 (followerId + followingId) |
| E-7 | `UserRoute` | Route 복제 (userId + spotLineId + scheduledDate + status) |

### 2.2 Backend — API 컨트롤러 (신규)

| # | 컨트롤러 | 엔드포인트 수 | 설명 |
|---|----------|:---:|------|
| C-1 | `SocialController` | 6 | Like/Save 토글 + 소셜 상태 조회 (Spot/Route 공통) |
| C-2 | `UserController` | 5 | 프로필 조회, 내 좋아요/저장 목록 |
| C-3 | `FollowController` | 5 | 팔로우/언팔로우, 상태, 팔로워/팔로잉 목록 |
| C-4 | `UserRouteController` | 4 | Route 복제, 내 Route 목록, 변형 목록 |

### 2.3 Backend — 서비스/유틸

| # | 항목 | 설명 |
|---|------|------|
| S-1 | `UserSyncService` | JWT의 userId + email → User 엔티티 자동 생성/조회 (첫 API 호출 시) |
| S-2 | `SocialService` | Like/Save 토글 로직 + count 동기화 |
| S-3 | `FollowService` | Follow/Unfollow + 팔로워/팔로잉 조회 |
| S-4 | `UserRouteService` | Route 복제 + 내 Route 관리 |
| S-5 | `AuthUtil` | SecurityContext에서 현재 userId 추출 유틸 |

### 2.4 범위 외 (Out of Scope)

- 댓글/답글 시스템 (향후 별도 feature)
- 알림 시스템 (Push/In-app)
- 소셜 피드 랭킹 알고리즘 (별도 feature: `feed-ranking-algorithm`)
- 경험 포스팅 + 방문 인증 (별도 feature: `experience-posting`)
- Front-spotLine 코드 변경 없음 (이미 완성)

---

## 3. 핵심 설계 결정

### 3.1 User 동기화 전략: Lazy Sync

- Supabase가 유저 관리 (회원가입, 로그인, 비밀번호)
- Spring Boot는 **첫 인증 API 호출 시** User 레코드 자동 생성
- JWT의 `sub` (UUID) + `email`로 User upsert
- 별도 회원가입 API 불필요

```
JWT 인증 → UserSyncService.getOrCreateUser(userId, email) → User 엔티티
```

### 3.2 Like/Save 토글: Idempotent Toggle

```
POST /api/v2/spots/{id}/like
→ 이미 좋아요 → 취소 (unlike) → { liked: false, likesCount: N-1 }
→ 좋아요 없음 → 등록 (like)  → { liked: true, likesCount: N+1 }
```

- Front의 optimistic update와 호환
- count는 엔티티의 likesCount 필드 직접 증감 (aggregate query 대신)

### 3.3 SecurityConfig 변경

현재 규칙에 추가 필요:
```
GET /api/v2/users/** → permitAll (프로필 공개 조회)
GET /api/v2/spots/{id}/social → permitAll (비로그인 시 count만 반환)
GET /api/v2/routes/{id}/social → permitAll
POST/DELETE 소셜 액션 → authenticated (기존 규칙으로 커버됨)
```

### 3.4 Front 호환성

Front가 이미 구현한 API 스펙에 **정확히 맞춰야** 함:
- 응답 필드명, 타입, 페이지네이션 구조 일치 필수
- Front의 `SocialToggleResponse`, `UserProfile`, `MyRoute` 타입에 맞춤

---

## 4. 구현 순서

```
E-1 User 엔티티 + Repository
  ↓
S-1 UserSyncService (JWT → User 자동 생성)
  ↓
S-5 AuthUtil (SecurityContext → userId)
  ↓
E-2~E-5 Like/Save 엔티티 + Repository
  ↓
S-2 SocialService (토글 로직)
  ↓
C-1 SocialController (Like/Save API)
  ↓
E-6 UserFollow 엔티티 + Repository
  ↓
S-3 FollowService
  ↓
C-3 FollowController
  ↓
C-2 UserController (프로필 + 목록)
  ↓
E-7 UserRoute 엔티티 + Repository
  ↓
S-4 UserRouteService
  ↓
C-4 UserRouteController (복제 + 내 Route)
  ↓
SecurityConfig 업데이트
```

---

## 5. 영향 범위

| 레포 | 파일 | 변경 유형 |
|------|------|----------|
| backend | `entity/User.java` | 신규 |
| backend | `entity/SpotLike.java` | 신규 |
| backend | `entity/SpotSave.java` | 신규 |
| backend | `entity/RouteLike.java` | 신규 |
| backend | `entity/RouteSave.java` | 신규 |
| backend | `entity/UserFollow.java` | 신규 |
| backend | `entity/UserRoute.java` | 신규 |
| backend | `repository/` (7개) | 신규 |
| backend | `service/UserSyncService.java` | 신규 |
| backend | `service/SocialService.java` | 신규 |
| backend | `service/FollowService.java` | 신규 |
| backend | `service/UserRouteService.java` | 신규 |
| backend | `security/AuthUtil.java` | 신규 |
| backend | `controller/SocialController.java` | 신규 |
| backend | `controller/UserController.java` | 신규 |
| backend | `controller/FollowController.java` | 신규 |
| backend | `controller/UserRouteController.java` | 신규 |
| backend | `dto/request/` (3~4개) | 신규 |
| backend | `dto/response/` (5~6개) | 신규 |
| backend | `config/SecurityConfig.java` | 수정 |
| front | 없음 | 변경 없음 (이미 완성) |

---

## 6. Front API 스펙 (정확히 맞춰야 할 응답 형식)

### SocialToggleResponse
```json
{
  "liked": true,
  "likesCount": 42,
  "savesCount": 15
}
```

### SocialStatus (GET /social)
```json
{
  "isLiked": true,
  "isSaved": false
}
```

### UserProfile
```json
{
  "id": "uuid",
  "nickname": "홍대맛집탐험가",
  "avatar": "https://...",
  "bio": "서울 카페 투어 중",
  "instagramId": "cafe_explorer",
  "stats": {
    "visited": 15,
    "liked": 42,
    "recommended": 3,
    "spotlines": 5,
    "followers": 120,
    "following": 80
  }
}
```

### MyRoute
```json
{
  "id": "uuid",
  "spotLineId": "uuid",
  "routeSlug": "hongdae-cafe-tour",
  "title": "홍대 카페투어",
  "area": "홍대",
  "spotsCount": 5,
  "scheduledDate": "2026-04-01",
  "status": "scheduled",
  "parentSpotLineId": "uuid"
}
```

### Paginated Response
```json
{
  "items": [...],
  "hasMore": true
}
```

---

## 7. 사전 조건

1. ✅ Supabase JWT 인증 완료 (backend-auth)
2. ✅ Spot/Route 엔티티에 count 필드 존재
3. ✅ Front 소셜 UI 완성
4. Supabase Dashboard에서 테스트용 유저 계정 2개 이상 필요 (팔로우 테스트)
