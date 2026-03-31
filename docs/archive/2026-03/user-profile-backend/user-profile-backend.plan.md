# user-profile-backend Plan

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | User 엔티티는 존재하지만 프로필 수정 API가 없어, 모든 유저가 email 앞부분 닉네임 + 빈 프로필로 고정됨 |
| **Solution** | PUT /users/me/profile (닉네임/bio/instagramId 수정) + 아바타 업로드 (기존 S3 인프라 활용) API 구현 |
| **Function/UX Effect** | 유저가 자신의 닉네임, 자기소개, 인스타그램 ID, 프로필 사진을 자유롭게 설정 가능 |
| **Core Value** | 소셜 기능의 완성도 향상 — 개인화된 프로필이 있어야 팔로우/피드 참여 의미가 생김 |

---

## 1. 현황 분석

### 1.1 Backend — 기존 구현

| 항목 | 상태 |
|------|:----:|
| User 엔티티 (id, email, nickname, avatar, bio, instagramId) | ✅ 존재 |
| UserRepository | ✅ 존재 |
| UserSyncService (JWT → User 자동생성) | ✅ 존재 |
| GET /users/{userId}/profile | ✅ 존재 |
| PUT /users/me/profile (프로필 수정) | ❌ 없음 |
| 아바타 업로드 API | ❌ 없음 |
| S3 Presigned URL 인프라 (MediaService) | ✅ 존재 |
| AuthUtil (SecurityContext → userId) | ✅ 존재 |

### 1.2 Frontend — 현재 상태

| 항목 | 상태 |
|------|:----:|
| /profile/me 페이지 (읽기 전용) | ✅ |
| /profile/[userId] 페이지 (읽기 전용) | ✅ |
| ProfileHeader 컴포넌트 (표시만) | ✅ |
| 프로필 수정 폼/모달 | ❌ 없음 |
| updateProfile() API 함수 | ❌ 없음 |
| 아바타 업로드 UI | ❌ 없음 |

### 1.3 Front가 기대하는 API (신규 정의)

```
PUT    /api/v2/users/me/profile          — 프로필 수정
POST   /api/v2/users/me/avatar           — 아바타 Presigned URL 발급
DELETE /api/v2/users/me/avatar           — 아바타 삭제
```

---

## 2. 구현 범위

### 2.1 Backend — API (신규)

| # | 엔드포인트 | 설명 |
|---|-----------|------|
| A-1 | `PUT /api/v2/users/me/profile` | 닉네임, bio, instagramId 수정 |
| A-2 | `POST /api/v2/users/me/avatar` | 아바타 이미지 Presigned URL 발급 (S3) |
| A-3 | `DELETE /api/v2/users/me/avatar` | 아바타 삭제 (S3 키 제거 + User.avatar null) |

### 2.2 Backend — 서비스/DTO

| # | 항목 | 설명 |
|---|------|------|
| S-1 | `UserProfileService` | 프로필 수정 로직 + 아바타 관리 |
| D-1 | `UpdateProfileRequest` | nickname, bio, instagramId |
| D-2 | `AvatarUploadResponse` | presignedUrl, avatarKey, avatarUrl |

### 2.3 Backend — 기존 파일 수정

| # | 파일 | 변경 |
|---|------|------|
| M-1 | `UserController.java` | 3개 엔드포인트 추가 |
| M-2 | `UserProfileResponse.java` | 수정 없음 (기존 from() 재사용) |

### 2.4 범위 외 (Out of Scope)

- Front 프로필 수정 UI (별도 feature: `user-profile-frontend`)
- 이메일 변경 (Supabase에서 관리)
- 비밀번호 변경 (Supabase에서 관리)
- 닉네임 중복 검사 (MVP에서 불필요 — 같은 닉네임 허용)

---

## 3. 핵심 설계 결정

### 3.1 프로필 수정: Partial Update

```
PUT /api/v2/users/me/profile
Body: { "nickname": "새닉네임", "bio": "자기소개", "instagramId": "insta_id" }
→ null 필드는 기존 값 유지 (partial update)
→ 빈 문자열("")은 해당 필드 초기화
```

### 3.2 아바타 업로드: 기존 S3 인프라 활용

- 기존 `MediaService` + `S3Service`에 Presigned URL 발급 기능 이미 존재
- 아바타 전용 S3 키: `avatars/{userId}/{timestamp}.{ext}`
- Flow: Front에서 Presigned URL 요청 → S3에 직접 업로드 → 완료 후 User.avatar 업데이트

### 3.3 UserSyncService 연동

- 프로필 수정 시 `UserSyncService.getOrCreateUser()` 호출하여 User 존재 보장
- JWT의 userId + email로 User가 없으면 자동 생성 후 수정 진행

### 3.4 Validation

- nickname: 1~30자, 필수
- bio: 0~200자, 선택
- instagramId: 0~50자, 선택, @없이 저장

---

## 4. 구현 순서

```
D-1 UpdateProfileRequest DTO
  ↓
D-2 AvatarUploadResponse DTO
  ↓
S-1 UserProfileService (수정 + 아바타 관리)
  ↓
M-1 UserController 엔드포인트 추가 (3개)
```

---

## 5. 영향 범위

| 레포 | 파일 | 변경 유형 |
|------|------|----------|
| backend | `dto/request/UpdateProfileRequest.java` | 신규 |
| backend | `dto/response/AvatarUploadResponse.java` | 신규 |
| backend | `service/UserProfileService.java` | 신규 |
| backend | `controller/UserController.java` | 수정 (3 엔드포인트 추가) |
| front | 없음 | 변경 없음 (별도 feature) |

총 **신규 3개, 수정 1개** — 소규모 feature

---

## 6. Front API 스펙 (응답 형식)

### PUT /users/me/profile — Response

```json
{
  "id": "uuid",
  "nickname": "새닉네임",
  "avatar": "https://...",
  "bio": "자기소개",
  "joinedAt": "2026-03-31T...",
  "instagramId": "insta_id",
  "stats": { "visited": 0, "liked": 5, "recommended": 0, "spotlines": 3, "followers": 10, "following": 8 }
}
```

→ 기존 `UserProfileResponse` 그대로 반환

### POST /users/me/avatar — Response

```json
{
  "presignedUrl": "https://s3....",
  "avatarKey": "avatars/uuid/1711878000.jpg",
  "avatarUrl": "https://cdn.../avatars/uuid/1711878000.jpg"
}
```

### DELETE /users/me/avatar — Response

```json
{
  "id": "uuid",
  "nickname": "닉네임",
  "avatar": null,
  ...
}
```

→ `UserProfileResponse` 반환 (avatar = null)

---

## 7. 사전 조건

1. ✅ User 엔티티 + UserRepository 존재
2. ✅ S3 Presigned URL 인프라 (MediaService, S3Service) 존재
3. ✅ AuthUtil (SecurityContext → userId) 존재
4. ✅ JWT 인증 완료 (Supabase)
