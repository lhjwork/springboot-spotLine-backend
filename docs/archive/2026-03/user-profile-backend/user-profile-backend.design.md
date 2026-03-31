# user-profile-backend Design Document

> **Summary**: 유저 프로필 수정 + 아바타 업로드/삭제 API 3개 구현
>
> **Project**: spotline-backend
> **Date**: 2026-03-31
> **Status**: Draft
> **Planning Doc**: [user-profile-backend.plan.md](../../01-plan/features/user-profile-backend.plan.md)

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | User 엔티티는 존재하지만 프로필 수정 API가 없어 유저가 닉네임/bio/avatar를 변경할 수 없음 |
| **Solution** | PUT /users/me/profile + POST/DELETE /users/me/avatar API 구현 (기존 S3 인프라 활용) |
| **Function/UX Effect** | 유저가 닉네임, 자기소개, 인스타그램 ID, 프로필 사진을 자유롭게 설정 |
| **Core Value** | 소셜 기능 완성도 향상 — 개인화된 프로필로 팔로우/피드 참여 의미 강화 |

---

## 1. Overview

### 1.1 Design Goals

- 기존 User 엔티티 + S3 인프라를 최대한 재사용하여 최소 코드로 구현
- Partial Update 지원 (null 필드는 기존 값 유지)
- 아바타 업로드는 Presigned URL 패턴으로 서버 부하 최소화

### 1.2 Design Principles

- 기존 패턴 준수: Controller → Service → Repository 흐름
- 기존 MediaService/S3Service의 Presigned URL 인프라 재사용
- UserSyncService로 User 존재 보장 (lazy sync)

---

## 2. Architecture

### 2.1 Data Flow

```
[프로필 수정]
Client → PUT /users/me/profile → UserProfileService.updateProfile() → UserRepository.save() → UserProfileResponse

[아바타 업로드]
Client → POST /users/me/avatar → UserProfileService.generateAvatarUploadUrl()
  → S3Service.generatePresignedUploadUrl() → AvatarUploadResponse (presignedUrl, avatarKey, avatarUrl)
Client → S3 직접 업로드 (presignedUrl 사용)
  → User.avatar = avatarUrl 자동 저장 (generateAvatarUploadUrl에서 미리 설정)

[아바타 삭제]
Client → DELETE /users/me/avatar → UserProfileService.deleteAvatar()
  → S3Service.deleteObject() + User.avatar = null → UserProfileResponse
```

### 2.2 Dependencies

| Component | Depends On | Purpose |
|-----------|-----------|---------|
| UserController | UserProfileService, AuthUtil | 3개 엔드포인트 추가 |
| UserProfileService | UserRepository, UserSyncService, S3Service, AuthUtil | 프로필 수정 + 아바타 관리 |
| UpdateProfileRequest | (없음) | 입력 DTO + Validation |
| AvatarUploadResponse | (없음) | Presigned URL 응답 DTO |

---

## 3. Data Model

### 3.1 User Entity (기존 — 변경 없음)

```java
@Entity @Table(name = "users")
public class User {
    @Id private String id;          // Supabase UUID
    private String email;           // unique
    private String nickname;        // 1~30자
    private String avatar;          // S3 public URL (nullable)
    private String bio;             // 0~200자 (nullable)
    private String instagramId;     // 0~50자 (nullable)
    private Integer followersCount; // default 0
    private Integer followingCount; // default 0
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3.2 아바타 S3 Key 구조

```
avatars/{userId}/{timestamp}.{ext}
```

예: `avatars/abc123-uuid/1711878000.jpg`

---

## 4. API Specification

### 4.1 Endpoint List

| # | Method | Path | Description | Auth |
|---|--------|------|-------------|------|
| A-1 | PUT | `/api/v2/users/me/profile` | 프로필 수정 | Required |
| A-2 | POST | `/api/v2/users/me/avatar` | 아바타 Presigned URL 발급 | Required |
| A-3 | DELETE | `/api/v2/users/me/avatar` | 아바타 삭제 | Required |

### 4.2 Detailed Specification

#### A-1: `PUT /api/v2/users/me/profile`

**Request:**
```json
{
  "nickname": "새닉네임",
  "bio": "자기소개",
  "instagramId": "insta_id"
}
```

- `nickname`: 1~30자, 필수 (null이면 기존 값 유지)
- `bio`: 0~200자, 선택 (null이면 기존 값 유지, ""이면 초기화)
- `instagramId`: 0~50자, 선택 (`@` 제거 후 저장)

**Response (200):** `UserProfileResponse` (기존 DTO 그대로)
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

**Error:**
- `400`: 유효성 검사 실패 (닉네임 길이, bio 길이)
- `401`: 인증 필요

#### A-2: `POST /api/v2/users/me/avatar`

**Request:**
```json
{
  "filename": "profile.jpg",
  "contentType": "image/jpeg"
}
```

**Response (200):**
```json
{
  "presignedUrl": "https://s3...",
  "avatarKey": "avatars/uuid/1711878000.jpg",
  "avatarUrl": "https://{bucket}.s3.{region}.amazonaws.com/avatars/uuid/1711878000.jpg"
}
```

**Error:**
- `400`: 지원하지 않는 파일 형식
- `401`: 인증 필요

#### A-3: `DELETE /api/v2/users/me/avatar`

**Request:** (없음)

**Response (200):** `UserProfileResponse` (avatar = null)

**Error:**
- `401`: 인증 필요

---

## 5. Implementation Details

### 5.1 D-1: UpdateProfileRequest DTO

**파일**: `dto/request/UpdateProfileRequest.java` (신규)

```java
package com.spotline.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 30, message = "닉네임은 1~30자여야 합니다")
    private String nickname;

    @Size(max = 200, message = "자기소개는 200자 이하여야 합니다")
    private String bio;

    @Size(max = 50, message = "인스타그램 ID는 50자 이하여야 합니다")
    private String instagramId;
}
```

### 5.2 D-2: AvatarUploadResponse DTO

**파일**: `dto/response/AvatarUploadResponse.java` (신규)

```java
package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarUploadResponse {
    private String presignedUrl;
    private String avatarKey;
    private String avatarUrl;
}
```

### 5.3 D-3: AvatarUploadRequest DTO

**파일**: `dto/request/AvatarUploadRequest.java` (신규)

```java
package com.spotline.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AvatarUploadRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "Content-Type은 필수입니다")
    private String contentType;
}
```

### 5.4 S-1: UserProfileService

**파일**: `service/UserProfileService.java` (신규)

```java
package com.spotline.api.service;

import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.repository.SpotLikeRepository;
import com.spotline.api.domain.repository.RouteSaveRepository;
import com.spotline.api.dto.request.UpdateProfileRequest;
import com.spotline.api.dto.response.AvatarUploadResponse;
import com.spotline.api.dto.response.UserProfileResponse;
import com.spotline.api.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserSyncService userSyncService;
    private final S3Service s3Service;
    private final SpotLikeRepository spotLikeRepository;
    private final RouteSaveRepository routeSaveRepository;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );

    @Transactional
    public UserProfileResponse updateProfile(String userId, String email, UpdateProfileRequest request) {
        User user = userSyncService.getOrCreateUser(userId, email);

        // Partial update: null이면 기존 값 유지
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isEmpty() ? null : request.getBio());
        }
        if (request.getInstagramId() != null) {
            String instaId = request.getInstagramId().startsWith("@")
                ? request.getInstagramId().substring(1)
                : request.getInstagramId();
            user.setInstagramId(instaId.isEmpty() ? null : instaId);
        }

        return buildProfileResponse(user);
    }

    @Transactional
    public AvatarUploadResponse generateAvatarUploadUrl(String userId, String email,
                                                         String filename, String contentType) {
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 가능합니다");
        }

        User user = userSyncService.getOrCreateUser(userId, email);

        // 기존 아바타 S3 키 삭제 (있으면)
        if (user.getAvatar() != null) {
            try {
                String oldKey = extractS3Key(user.getAvatar());
                s3Service.deleteObject(oldKey);
            } catch (Exception ignored) {
                // 기존 아바타 삭제 실패해도 계속 진행
            }
        }

        // 새 아바타 키 생성
        String ext = extractExtension(filename);
        String avatarKey = "avatars/" + userId + "/" + System.currentTimeMillis() + "." + ext;
        String presignedUrl = s3Service.generatePresignedUploadUrl(avatarKey, contentType);
        String avatarUrl = s3Service.getPublicUrl(avatarKey);

        // User.avatar 미리 업데이트 (Client가 presignedUrl로 S3 업로드 성공할 것으로 가정)
        user.setAvatar(avatarUrl);

        return AvatarUploadResponse.builder()
            .presignedUrl(presignedUrl)
            .avatarKey(avatarKey)
            .avatarUrl(avatarUrl)
            .build();
    }

    @Transactional
    public UserProfileResponse deleteAvatar(String userId, String email) {
        User user = userSyncService.getOrCreateUser(userId, email);

        if (user.getAvatar() != null) {
            try {
                String oldKey = extractS3Key(user.getAvatar());
                s3Service.deleteObject(oldKey);
            } catch (Exception ignored) {
                // S3 삭제 실패해도 DB에서는 null로 설정
            }
            user.setAvatar(null);
        }

        return buildProfileResponse(user);
    }

    private UserProfileResponse buildProfileResponse(User user) {
        int likedCount = (int) spotLikeRepository.countByUserId(user.getId());
        int savedCount = (int) routeSaveRepository.countByUserId(user.getId());
        return UserProfileResponse.from(user, likedCount, savedCount);
    }

    private String extractS3Key(String url) {
        // https://{bucket}.s3.{region}.amazonaws.com/{key} → {key}
        int idx = url.indexOf(".amazonaws.com/");
        return idx >= 0 ? url.substring(idx + 15) : url;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1).toLowerCase() : "jpg";
    }
}
```

### 5.5 M-1: UserController 수정

**파일**: `controller/UserController.java` (수정 — 3개 엔드포인트 추가)

```java
// === 추가할 필드 ===
private final UserProfileService userProfileService;

// === 추가할 엔드포인트 3개 ===

@PutMapping("/me/profile")
public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
    return userProfileService.updateProfile(
        authUtil.requireUserId(), authUtil.getCurrentEmail(), request);
}

@PostMapping("/me/avatar")
public AvatarUploadResponse uploadAvatar(@Valid @RequestBody AvatarUploadRequest request) {
    return userProfileService.generateAvatarUploadUrl(
        authUtil.requireUserId(), authUtil.getCurrentEmail(),
        request.getFilename(), request.getContentType());
}

@DeleteMapping("/me/avatar")
public UserProfileResponse deleteAvatar() {
    return userProfileService.deleteAvatar(
        authUtil.requireUserId(), authUtil.getCurrentEmail());
}
```

---

## 6. Error Handling

| Code | Endpoint | Cause | Message |
|------|----------|-------|---------|
| 400 | PUT /me/profile | 닉네임 길이 초과 | "닉네임은 1~30자여야 합니다" |
| 400 | PUT /me/profile | bio 길이 초과 | "자기소개는 200자 이하여야 합니다" |
| 400 | POST /me/avatar | 지원하지 않는 이미지 형식 | "지원하지 않는 이미지 형식입니다" |
| 401 | All | 인증 미제공 | Spring Security 기본 처리 |

---

## 7. Security Considerations

- [x] `authUtil.requireUserId()`로 인증 강제 — 본인 프로필만 수정 가능
- [x] SecurityConfig에서 PUT/POST/DELETE `/api/v2/**`는 이미 authenticated() 설정됨
- [x] @Valid + Bean Validation으로 입력 검증
- [x] instagramId에서 `@` 문자 자동 제거 (XSS 방지)
- [x] S3 presigned URL 10분 만료

---

## 8. Repository 추가 메서드

SpotLikeRepository, RouteSaveRepository에 `countByUserId` 메서드 필요:

```java
// SpotLikeRepository — 추가
long countByUserId(String userId);

// RouteSaveRepository — 추가
long countByUserId(String userId);
```

---

## 9. Implementation Order

```
1. [ ] D-1: UpdateProfileRequest DTO
2. [ ] D-2: AvatarUploadResponse DTO
3. [ ] D-3: AvatarUploadRequest DTO
4. [ ] R-1: SpotLikeRepository.countByUserId 추가
5. [ ] R-2: RouteSaveRepository.countByUserId 추가
6. [ ] S-1: UserProfileService (수정 + 아바타 관리)
7. [ ] M-1: UserController 엔드포인트 추가 (3개)
```

---

## 10. Impact Analysis

| 파일 | 변경 유형 | 변경 내용 |
|------|----------|----------|
| `dto/request/UpdateProfileRequest.java` | 신규 | 프로필 수정 요청 DTO |
| `dto/request/AvatarUploadRequest.java` | 신규 | 아바타 업로드 요청 DTO |
| `dto/response/AvatarUploadResponse.java` | 신규 | Presigned URL 응답 DTO |
| `service/UserProfileService.java` | 신규 | 프로필 수정 + 아바타 관리 서비스 |
| `controller/UserController.java` | 수정 | 3개 엔드포인트 추가 |
| `domain/repository/SpotLikeRepository.java` | 수정 | countByUserId 추가 |
| `domain/repository/RouteSaveRepository.java` | 수정 | countByUserId 추가 |

총 **신규 4개, 수정 3개**

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1 | 2026-03-31 | Initial draft |
