# backend-api-docs Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: springboot-spotLine-backend
> **Version**: 0.0.1-SNAPSHOT
> **Analyst**: Claude (bkit PDCA)
> **Date**: 2026-04-04
> **Design Doc**: [backend-api-docs.design.md](../02-design/features/backend-api-docs.design.md)

---

## 1. Executive Summary

Match Rate: **97%** -- 2 minor gaps out of 7 design steps fully verified. All infrastructure (dependency, config, security, prod-disable) is verbatim. All 14 controllers have correct `@Tag` + `@Operation` annotations (71 total). All Request/Response DTOs have class-level `@Schema`. Two field-level `@Schema` annotations are missing on CreateSpotRequest.mediaItems and CreateRouteRequest.spots. API_DOCUMENTATION.md exists at shared `docs/` (not backend-local), which is acceptable given the monorepo structure.

---

## 2. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Step 1: build.gradle dependency | 100% | PASS |
| Step 2: OpenApiConfig.java | 100% | PASS |
| Step 3: Properties + Security + Prod | 100% | PASS |
| Step 4: Controller @Tag + @Operation (14) | 100% | PASS |
| Step 5: Request DTO @Schema | 93% | WARN |
| Step 6: Response DTO @Schema | 100% | PASS |
| Step 7: API_DOCUMENTATION.md v2 | 95% | WARN |
| **Overall** | **97%** | PASS |

---

## 3. Step-by-Step Comparison

### Step 1: build.gradle -- springdoc-openapi dependency

| Item | Design | Implementation | Match |
|------|--------|----------------|:-----:|
| Dependency | `springdoc-openapi-starter-webmvc-ui:2.8.4` | `springdoc-openapi-starter-webmvc-ui:2.8.4` | PASS |
| Location | `build.gradle` dependencies block | Line 49 | PASS |

**Verdict**: Verbatim match.

---

### Step 2: OpenApiConfig.java

| Item | Design | Implementation | Match |
|------|--------|----------------|:-----:|
| File path | `config/OpenApiConfig.java` | `config/OpenApiConfig.java` | PASS |
| @Configuration | Yes | Yes | PASS |
| Bean name | `spotlineOpenAPI()` | `spotlineOpenAPI()` | PASS |
| Title | "Spotline API" | "Spotline API" | PASS |
| Description | "Experience Based Social Platform..." | Identical | PASS |
| Version | "2.0" | "2.0" | PASS |
| Contact | "Spotline Team" | "Spotline Team" | PASS |
| SecurityScheme name | "bearerAuth" | "bearerAuth" | PASS |
| SecurityScheme type | HTTP / bearer / JWT | HTTP / bearer / JWT | PASS |
| SecurityScheme description | "Supabase JWT Access Token" | "Supabase JWT Access Token" | PASS |
| Global SecurityRequirement | Yes | Yes | PASS |

**Verdict**: Verbatim match. All imports, structure, and values identical.

---

### Step 3: application.properties + SecurityConfig + Prod

#### 3a. application.properties springdoc settings

| Property | Design | Implementation | Match |
|----------|--------|----------------|:-----:|
| `springdoc.api-docs.path` | `/v3/api-docs` | `/v3/api-docs` | PASS |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | `/swagger-ui.html` | PASS |
| `springdoc.swagger-ui.tags-sorter` | `alpha` | `alpha` | PASS |
| `springdoc.swagger-ui.operations-sorter` | `method` | `method` | PASS |
| `springdoc.swagger-ui.doc-expansion` | `list` | `list` | PASS |
| `springdoc.default-produces-media-type` | `application/json` | `application/json` | PASS |
| `springdoc.default-consumes-media-type` | `application/json` | `application/json` | PASS |

#### 3b. SecurityConfig swagger permitAll

| Item | Design | Implementation | Match |
|------|--------|----------------|:-----:|
| Matchers | `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | `.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()` (line 37) | PASS |

#### 3c. application-prod.properties

| Property | Design | Implementation | Match |
|----------|--------|----------------|:-----:|
| `springdoc.api-docs.enabled` | `false` | `false` | PASS |
| `springdoc.swagger-ui.enabled` | `false` | `false` | PASS |

**Verdict**: All 3 sub-steps verbatim match.

---

### Step 4: Controller @Tag + @Operation Annotations (14 Controllers)

| # | Controller | @Tag | @Operation Count (Design) | @Operation Count (Actual) | Match |
|---|-----------|------|:---:|:---:|:-----:|
| 1 | SpotController | `Spot` / "스팟 CRUD + 탐색" | 10 | 10 | PASS |
| 2 | RouteController | `Route` / "루트 CRUD + 탐색" | 6 | 6 | PASS |
| 3 | PlaceController | `Place` / "네이버/카카오 Place API 프록시 (24h 캐싱)" | 2 | 2 | PASS |
| 4 | MediaController | `Media` / "미디어 업로드 (S3 Presigned URL)" | 2 | 2 | PASS |
| 5 | SocialController | `Social` / "좋아요/저장 토글" | 6 | 6 | PASS |
| 6 | CommentController | `Comment` / "댓글 CRUD (스팟/루트)" | 4 | 4 | PASS |
| 7 | FollowController | `Follow` / "사용자 팔로우/팔로워" | 5 | 5 | PASS |
| 8 | UserController | `User` / "사용자 프로필 + 내 컨텐츠" | 9 | 9 | PASS |
| 9 | UserRouteController | `UserRoute` / "루트 복제 + 내 루트 관리" | 5 | 5 | PASS |
| 10 | QrScanController | `QR` / "QR 스캔 기록" | 1 | 1 | PASS |
| 11 | AnalyticsController | `Analytics` / "조회수 + 관리자 통계" | 6 | 6 | PASS |
| 12 | ContentReportController | `Report` / "콘텐츠 신고 + 관리자 처리" | 4 | 4 | PASS |
| 13 | PartnerController | `Partner` / "QR 파트너 매장 관리 (관리자)" | 10 | 10 | PASS |
| 14 | HealthController | `Health` / "헬스체크" | 1 | 1 | PASS |
| | **Total** | | **71** | **71** | PASS |

All @Tag names and descriptions match design verbatim. All @Operation summary strings match design verbatim.

**Verdict**: 14/14 controllers, 71/71 operations. Perfect match.

---

### Step 5: Request DTO @Schema Annotations

#### 5.1 Class-level @Schema

| DTO | Design Description | Actual Description | Match |
|-----|-------------------|-------------------|:-----:|
| CreateSpotRequest | "스팟 생성 요청" | "스팟 생성 요청" | PASS |
| CreateRouteRequest | "루트 생성 요청" | "루트 생성 요청" | PASS |
| PresignedUrlRequest | "S3 업로드 Presigned URL 요청" | "S3 업로드 Presigned URL 요청" | PASS |
| CreateCommentRequest | "댓글 작성 요청" | "댓글 작성 요청" | PASS |
| CreatePartnerRequest | "파트너 등록 요청" | "파트너 등록 요청" | PASS |
| CreateReportRequest | "콘텐츠 신고 요청" | "콘텐츠 신고 요청" | PASS |
| UpdateProfileRequest | "프로필 수정 요청" | "프로필 수정 요청" | PASS |
| UpdateSpotRequest | "스팟 수정 요청" | "스팟 수정 요청" | PASS |
| UpdateRouteRequest | "루트 수정 요청" | "루트 수정 요청" | PASS |
| ReplicateRouteRequest | "루트 복제 요청" | "루트 복제 요청" | PASS |
| AvatarUploadRequest | "아바타 업로드 요청" | "아바타 업로드 요청" | PASS |
| MediaItemRequest | "미디어 아이템 정보" | "미디어 아이템 정보" | PASS |

**12/12 class-level @Schema -- all match.**

#### 5.2 Field-level @Schema (CreateSpotRequest)

| Field | Design @Schema | Actual @Schema | Match |
|-------|---------------|---------------|:-----:|
| title | description + example + requiredMode | Identical | PASS |
| category | description + example | Identical | PASS |
| source | description + example | Identical | PASS |
| crewNote | description + example | Identical | PASS |
| address | description + example | Identical | PASS |
| latitude | description + example | Identical | PASS |
| longitude | description + example | Identical | PASS |
| area | description + example | Identical | PASS |
| tags | description + example | Identical | PASS |
| mediaItems | `@Schema(description = "미디어 아이템")` | **MISSING** | FAIL |
| creatorName | description + example | Identical | PASS |

#### 5.3 Field-level @Schema (CreateRouteRequest)

| Field | Design @Schema | Actual @Schema | Match |
|-------|---------------|---------------|:-----:|
| title | description + example | Identical | PASS |
| description | description | Identical | PASS |
| theme | description + example | Identical | PASS |
| area | description + example | Identical | PASS |
| spots | `@Schema(description = "스팟 목록 (순서대로)")` | **MISSING** | FAIL |

#### 5.4 Beneficial Extras (not in design, present in implementation)

| DTO | @Schema | Note |
|-----|---------|------|
| UpdateCommentRequest | "댓글 수정 요청" | Not listed in design Step 5.3 table |
| UpdatePartnerRequest | "파트너 수정 요청" | Not listed in design Step 5.3 table |
| UpdateMyRouteStatusRequest | "내 루트 상태 변경 요청" | Not listed in design Step 5.3 table |
| ResolveReportRequest | "신고 처리 요청" | Not listed in design Step 5.3 table |
| CreateQrCodeRequest | "QR 코드 생성 요청" | Not listed in design Step 5.3 table |

These 5 extras are beneficial -- more complete documentation than design required.

**Verdict**: 2 missing field-level @Schema annotations. 5 beneficial extras.

---

### Step 6: Response DTO @Schema Annotations

| DTO | Design Description | Actual Description | Match |
|-----|-------------------|-------------------|:-----:|
| SpotDetailResponse | "스팟 상세 응답" | "스팟 상세 응답" | PASS |
| RouteDetailResponse | "루트 상세 응답" | "루트 상세 응답" | PASS |
| RoutePreviewResponse | "루트 미리보기 응답" | "루트 미리보기 응답" | PASS |
| DiscoverResponse | "QR Discovery 응답" | "QR Discovery 응답" | PASS |
| SocialStatusResponse | "소셜 상태 응답 (좋아요/저장)" | "소셜 상태 응답 (좋아요/저장)" | PASS |
| SocialToggleResponse | "소셜 토글 결과" | "소셜 토글 결과" | PASS |
| CommentResponse | "댓글 응답" | "댓글 응답" | PASS |
| UserProfileResponse | "사용자 프로필 응답" | "사용자 프로필 응답" | PASS |
| MyRouteResponse | "내 루트 응답" | "내 루트 응답" | PASS |
| PresignedUrlResponse | "S3 Presigned URL 응답" | "S3 Presigned URL 응답" | PASS |
| PartnerResponse | "파트너 상세 응답" | "파트너 상세 응답" | PASS |
| ReportResponse | "신고 상세 응답" | "신고 상세 응답" | PASS |
| PlatformStatsResponse | "플랫폼 통계 응답" | "플랫폼 통계 응답" | PASS |
| PopularContentResponse | "인기 콘텐츠 응답" | "인기 콘텐츠 응답" | PASS |
| SimplePageResponse | "간단 페이지네이션 응답" | "간단 페이지네이션 응답" | PASS |
| SlugResponse | "Slug 응답 (SSR/sitemap)" | "Slug 응답 (SSR/sitemap)" | PASS |
| FollowResponse | "팔로우 결과" | "팔로우 결과" | PASS |
| FollowStatusResponse | "팔로우 상태" | "팔로우 상태" | PASS |
| ReplicateRouteResponse | "루트 복제 결과" | "루트 복제 결과" | PASS |
| AvatarUploadResponse | "아바타 업로드 결과" | "아바타 업로드 결과" | PASS |

**20/20 Response DTO @Schema -- all match.**

Beneficial extras (not in design):
- PartnerAnalyticsResponse: "파트너 분석 응답"
- SpotMediaResponse: "스팟 미디어 응답"
- SpotPartnerInfo: "스팟 파트너 정보"
- PartnerQrCodeResponse: "파트너 QR 코드 응답"
- DailyContentTrendResponse: "일별 콘텐츠 추이 응답"

**Verdict**: Perfect match + 5 beneficial extras.

---

### Step 7: docs/API_DOCUMENTATION.md v2

| Item | Design | Implementation | Match |
|------|--------|----------------|:-----:|
| File exists | Yes | Yes (at `qrAd/docs/API_DOCUMENTATION.md`) | PASS |
| v2 content | Spring Boot base URL, Swagger UI link | Present | PASS |
| Base URL | `http://localhost:4000/api/v2` | `http://localhost:4000/api/v2` | PASS |
| Swagger UI link | `http://localhost:4000/swagger-ui.html` | `http://localhost:4000/swagger-ui.html` | PASS |
| Auth description | Supabase JWT | Present | PASS |
| API group summary table | 8+ groups | 12 groups with endpoint counts | PASS |
| Error format | Documented | Documented | PASS |
| Location | `docs/API_DOCUMENTATION.md` (design implies backend-local) | `qrAd/docs/` (shared monorepo docs) | WARN |

**Verdict**: Content is complete v2. Location is shared `docs/` rather than backend-local, which is the established project convention per CLAUDE.md monorepo structure.

---

## 4. Differences Found

### FAIL Missing (Design O, Implementation X)

| # | Item | Design Location | Implementation Location | Description |
|---|------|-----------------|------------------------|-------------|
| 1 | CreateSpotRequest.mediaItems @Schema | design.md Step 5.1 | `dto/request/CreateSpotRequest.java:62` | Missing `@Schema(description = "미디어 아이템")` on field |
| 2 | CreateRouteRequest.spots @Schema | design.md Step 5.2 | `dto/request/CreateRouteRequest.java:32` | Missing `@Schema(description = "스팟 목록 (순서대로)")` on field |

### PASS Beneficial Extras (Design X, Implementation O)

| # | Item | Implementation Location | Description |
|---|------|------------------------|-------------|
| 1-5 | 5 extra Request DTO @Schema | Various request DTOs | UpdateCommentRequest, UpdatePartnerRequest, UpdateMyRouteStatusRequest, ResolveReportRequest, CreateQrCodeRequest |
| 6-10 | 5 extra Response DTO @Schema | Various response DTOs | PartnerAnalyticsResponse, SpotMediaResponse, SpotPartnerInfo, PartnerQrCodeResponse, DailyContentTrendResponse |

---

## 5. Recommended Actions

### Immediate (to reach 100%)

1. **Add `@Schema(description = "미디어 아이템")` to `CreateSpotRequest.mediaItems`** (line 62)
2. **Add `@Schema(description = "스팟 목록 (순서대로)")` to `CreateRouteRequest.spots`** (line 32)

These are two single-line additions. Estimated effort: < 1 minute.

### Documentation Update

1. Update design doc Step 5.3 table to include the 5 beneficial extra Request DTOs already implemented
2. Update design doc Step 6 table to include the 5 beneficial extra Response DTOs already implemented
3. Note in design doc that `API_DOCUMENTATION.md` lives at shared `qrAd/docs/` path

---

## 6. File Reference

| File | Path |
|------|------|
| Design Document | `docs/02-design/features/backend-api-docs.design.md` |
| build.gradle | `build.gradle` |
| OpenApiConfig | `src/main/java/com/spotline/api/config/OpenApiConfig.java` |
| application.properties | `src/main/resources/application.properties` |
| application-prod.properties | `src/main/resources/application-prod.properties` |
| SecurityConfig | `src/main/java/com/spotline/api/config/SecurityConfig.java` |
| CreateSpotRequest | `src/main/java/com/spotline/api/dto/request/CreateSpotRequest.java` |
| CreateRouteRequest | `src/main/java/com/spotline/api/dto/request/CreateRouteRequest.java` |
| API_DOCUMENTATION.md | `../docs/API_DOCUMENTATION.md` (shared monorepo) |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-04 | Initial gap analysis | Claude (bkit PDCA) |
