# user-profile-backend Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: spotline-backend
> **Analyst**: gap-detector
> **Date**: 2026-03-31
> **Design Doc**: [user-profile-backend.design.md](../02-design/features/user-profile-backend.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Design document (Section 9 Implementation Order) 7개 항목과 실제 구현 코드의 일치율을 검증한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/user-profile-backend.design.md`
- **Implementation Path**: `src/main/java/com/spotline/api/`
- **Analysis Date**: 2026-03-31
- **Design Items**: 7 (D-1, D-2, D-3, R-1, R-2, S-1, M-1)

---

## 2. Gap Analysis (Design vs Implementation)

### 2.1 Design Checklist Verification

| # | Item | Design | Implementation | Status |
|---|------|--------|----------------|--------|
| D-1 | UpdateProfileRequest DTO | nickname @Size(1,30), bio @Size(max=200), instagramId @Size(max=50) | Exact match | ✅ Match |
| D-2 | AvatarUploadResponse DTO | @Data @Builder, presignedUrl/avatarKey/avatarUrl | Exact match | ✅ Match |
| D-3 | AvatarUploadRequest DTO | filename @NotBlank, contentType @NotBlank | Exact match | ✅ Match |
| R-1 | SpotLikeRepository.countByUserId | `long countByUserId(String userId)` | Exact match | ✅ Match |
| R-2 | RouteSaveRepository.countByUserId | `long countByUserId(String userId)` | Exact match | ✅ Match |
| S-1 | UserProfileService | updateProfile (partial), generateAvatarUploadUrl (ALLOWED_IMAGE_TYPES), deleteAvatar (S3 cleanup) | Exact match | ✅ Match |
| M-1 | UserController 3 endpoints | PUT /me/profile, POST /me/avatar, DELETE /me/avatar | Exact match | ✅ Match |

### 2.2 API Endpoints

| Design Endpoint | Implementation | Status | Notes |
|-----------------|----------------|--------|-------|
| PUT `/api/v2/users/me/profile` | `@PutMapping("/me/profile")` | ✅ Match | @Valid + @RequestBody |
| POST `/api/v2/users/me/avatar` | `@PostMapping("/me/avatar")` | ✅ Match | @Valid + @RequestBody |
| DELETE `/api/v2/users/me/avatar` | `@DeleteMapping("/me/avatar")` | ✅ Match | No request body |

### 2.3 DTO Field-Level Verification

**UpdateProfileRequest**

| Field | Design Annotation | Implementation | Status |
|-------|-------------------|----------------|--------|
| nickname | `@Size(min=1, max=30)` | `@Size(min = 1, max = 30, message = "...")` | ✅ |
| bio | `@Size(max=200)` | `@Size(max = 200, message = "...")` | ✅ |
| instagramId | `@Size(max=50)` | `@Size(max = 50, message = "...")` | ✅ |

**AvatarUploadRequest**

| Field | Design Annotation | Implementation | Status |
|-------|-------------------|----------------|--------|
| filename | `@NotBlank` | `@NotBlank(message = "...")` | ✅ |
| contentType | `@NotBlank` | `@NotBlank(message = "...")` | ✅ |

**AvatarUploadResponse**

| Field | Design | Implementation | Status |
|-------|--------|----------------|--------|
| presignedUrl | String | String | ✅ |
| avatarKey | String | String | ✅ |
| avatarUrl | String | String | ✅ |

### 2.4 Service Logic Verification

| Logic | Design | Implementation | Status |
|-------|--------|----------------|--------|
| Partial update (null = keep) | nickname/bio/instagramId null check | Lines 35-46 in UserProfileService | ✅ |
| Bio empty string -> null | `bio.isEmpty() ? null : bio` | Line 39 | ✅ |
| Instagram `@` removal | `startsWith("@") -> substring(1)` | Lines 42-44 | ✅ |
| Instagram empty -> null | `instaId.isEmpty() ? null : instaId` | Line 45 | ✅ |
| ALLOWED_IMAGE_TYPES | jpeg, png, webp | Line 27-29 | ✅ |
| Old avatar S3 cleanup | `deleteObject(extractS3Key())` | Lines 61-67 | ✅ |
| Avatar key format | `avatars/{userId}/{timestamp}.{ext}` | Line 70 | ✅ |
| Presigned URL generation | `s3Service.generatePresignedUploadUrl()` | Line 71 | ✅ |
| Pre-save avatar URL | `user.setAvatar(avatarUrl)` | Line 74 | ✅ |
| Delete avatar S3 + null | `deleteObject()` + `setAvatar(null)` | Lines 88-94 | ✅ |
| buildProfileResponse | countByUserId for likes/saves | Lines 99-103 | ✅ |

### 2.5 Match Rate Summary

```
+---------------------------------------------+
|  Overall Match Rate: 100%                    |
+---------------------------------------------+
|  ✅ Match:           7/7 items (100%)        |
|  ⚠️ Minor deviation: 0 items (0%)           |
|  ❌ Not implemented:  0 items (0%)           |
+---------------------------------------------+
```

---

## 3. Code Quality Analysis

### 3.1 Code Smells

None found. All methods are concise and focused.

### 3.2 Security Verification

| Item | Design Requirement | Implementation | Status |
|------|-------------------|----------------|--------|
| Auth enforcement | `authUtil.requireUserId()` | All 3 endpoints call requireUserId() | ✅ |
| Input validation | `@Valid` + Bean Validation | All endpoints use @Valid @RequestBody | ✅ |
| `@` removal | XSS prevention | `startsWith("@") -> substring(1)` | ✅ |
| S3 error tolerance | Catch + continue | `catch (Exception ignored)` blocks | ✅ |

---

## 4. Architecture Compliance

### 4.1 Layer Dependency Verification

| Layer | Expected Dependencies | Actual Dependencies | Status |
|-------|----------------------|---------------------|--------|
| Controller | Service, AuthUtil, DTOs | UserProfileService, AuthUtil, DTOs | ✅ |
| Service | Repository, UserSyncService, S3Service | Exact match | ✅ |
| Repository | JpaRepository, Entity | Exact match | ✅ |
| DTO | None (standalone) | None | ✅ |

### 4.2 File Placement

| Component | Design Location | Actual Location | Status |
|-----------|----------------|-----------------|--------|
| UpdateProfileRequest | `dto/request/` | `dto/request/UpdateProfileRequest.java` | ✅ |
| AvatarUploadRequest | `dto/request/` | `dto/request/AvatarUploadRequest.java` | ✅ |
| AvatarUploadResponse | `dto/response/` | `dto/response/AvatarUploadResponse.java` | ✅ |
| UserProfileService | `service/` | `service/UserProfileService.java` | ✅ |
| UserController | `controller/` | `controller/UserController.java` | ✅ |

---

## 5. Convention Compliance

### 5.1 Naming Convention

| Category | Convention | Compliance |
|----------|-----------|:----------:|
| Class names | PascalCase | 100% |
| Method names | camelCase | 100% |
| Constants | UPPER_SNAKE_CASE | 100% (ALLOWED_IMAGE_TYPES) |
| Files | PascalCase.java | 100% |

### 5.2 Import Order

All files follow: `java.*` -> `org.springframework.*` -> `com.spotline.*` -> `lombok.*`. Compliant.

### 5.3 Korean Error Messages

| Message | Location | Compliant |
|---------|----------|:---------:|
| "닉네임은 1~30자여야 합니다" | UpdateProfileRequest | ✅ |
| "자기소개는 200자 이하여야 합니다" | UpdateProfileRequest | ✅ |
| "인스타그램 ID는 50자 이하여야 합니다" | UpdateProfileRequest | ✅ |
| "파일명은 필수입니다" | AvatarUploadRequest | ✅ |
| "Content-Type은 필수입니다" | AvatarUploadRequest | ✅ |
| "지원하지 않는 이미지 형식입니다..." | UserProfileService | ✅ |

---

## 6. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 100% | ✅ |
| Architecture Compliance | 100% | ✅ |
| Convention Compliance | 100% | ✅ |
| **Overall** | **100%** | ✅ |

---

## 7. Findings Summary

### Missing Features (Design O, Implementation X)

None.

### Added Features (Design X, Implementation O)

None. The implementation strictly follows the design.

### Changed Features (Design != Implementation)

None. All 7 design items are implemented verbatim.

---

## 8. Recommended Actions

No actions required. Design and implementation are fully aligned.

### Documentation Sync

- Design document Section 9 checklist can be marked as all complete.

---

## 9. Next Steps

- [x] Gap analysis complete (100% match)
- [ ] Mark design document checklist items as done
- [ ] Write completion report (`/pdca report user-profile-backend`)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-31 | Initial analysis -- 100% match rate | gap-detector |
