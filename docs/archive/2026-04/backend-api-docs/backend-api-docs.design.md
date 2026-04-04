# backend-api-docs Design Document

> **Summary**: Spring Boot 백엔드에 springdoc-openapi 기반 Swagger UI + API 문서 자동 생성
>
> **Project**: springboot-spotLine-backend
> **Version**: 0.0.1-SNAPSHOT
> **Author**: Claude (bkit PDCA)
> **Date**: 2026-04-04
> **Status**: Draft
> **Planning Doc**: [backend-api-docs.plan.md](../../01-plan/features/backend-api-docs.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- springdoc-openapi로 16개 컨트롤러 + 46개 DTO 자동 문서화
- Swagger UI에서 JWT 인증 후 API 테스트 가능
- dev 프로필에서만 활성화 (prod 비활성화)
- 간결한 어노테이션으로 컨트롤러 가독성 유지

### 1.2 Design Principles

- **Minimal Annotation**: `@Tag` + `@Operation(summary)` 수준으로 간결하게
- **Auto-Discovery**: springdoc이 기존 `@RestController`, `@RequestMapping`, validation 어노테이션을 자동 감지
- **Security First**: prod에서는 Swagger UI 완전 비활성화

---

## 2. Implementation Steps

### Step 1: build.gradle — 의존성 추가

**File**: `build.gradle`

**변경 내용**: dependencies 블록에 springdoc-openapi 추가

```groovy
// OpenAPI / Swagger UI
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4'
```

**검증**: `./gradlew dependencies | grep springdoc`

---

### Step 2: OpenApiConfig.java — 설정 클래스 생성

**File**: `src/main/java/com/spotline/api/config/OpenApiConfig.java` (NEW)

```java
package com.spotline.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI spotlineOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Spotline API")
                .description("Experience Based Social Platform — Spot/Route/Place/Social/QR API")
                .version("2.0")
                .contact(new Contact().name("Spotline Team")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Supabase JWT Access Token")));
    }
}
```

**핵심 결정**:
- Global SecurityRequirement로 모든 엔드포인트에 자물쇠 아이콘 표시
- 개별 공개 API는 `@SecurityRequirements` 빈 배열로 오버라이드

---

### Step 3: application.properties — springdoc 설정

**File**: `src/main/resources/application.properties`

**추가할 설정**:

```properties
# === OpenAPI / Swagger UI ===
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.doc-expansion=list
springdoc.default-produces-media-type=application/json
springdoc.default-consumes-media-type=application/json
```

**SecurityConfig 수정**: Swagger UI 경로를 public으로 허용

```java
// SecurityConfig.java — authorizeHttpRequests 블록에 추가
.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
```

**prod 비활성화**: `application-prod.properties` (존재하면 추가, 없으면 생성)

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

---

### Step 4: 컨트롤러 @Tag + @Operation 어노테이션

각 컨트롤러에 클래스 레벨 `@Tag`와 주요 메서드에 `@Operation(summary=...)` 추가.

#### 4.1 SpotController

```java
@Tag(name = "Spot", description = "스팟 CRUD + 탐색")
@RestController
@RequestMapping("/api/v2/spots")
public class SpotController {

    @Operation(summary = "스팟 목록 조회")
    @GetMapping("")
    // listSpots(...)

    @Operation(summary = "스팟 상세 조회 (slug)")
    @GetMapping("/{slug}")
    // getSpotBySlug(...)

    @Operation(summary = "근처 스팟 조회")
    @GetMapping("/nearby")
    // getNearbySpots(...)

    @Operation(summary = "QR Discovery — 현재 스팟 기반 추천")
    @GetMapping("/discover")
    // discoverSpots(...)

    @Operation(summary = "스팟이 포함된 루트 목록")
    @GetMapping("/{spotId}/routes")
    // getRoutesForSpot(...)

    @Operation(summary = "전체 스팟 slug 목록 (SSR/sitemap)")
    @GetMapping("/slugs")
    // getAllSlugs()

    @Operation(summary = "스팟 생성")
    @PostMapping("")
    // createSpot(...)

    @Operation(summary = "스팟 대량 생성 (최대 50개)")
    @PostMapping("/bulk")
    // bulkCreateSpots(...)

    @Operation(summary = "스팟 수정")
    @PutMapping("/{slug}")
    // updateSpot(...)

    @Operation(summary = "스팟 삭제")
    @DeleteMapping("/{slug}")
    // deleteSpot(...)
}
```

#### 4.2 RouteController

```java
@Tag(name = "Route", description = "루트 CRUD + 탐색")
```

| Method | Summary |
|--------|---------|
| `GET /popular` | 인기 루트 목록 조회 |
| `GET /{slug}` | 루트 상세 조회 (slug) |
| `GET /slugs` | 전체 루트 slug 목록 (SSR/sitemap) |
| `POST /` | 루트 생성 |
| `PUT /{slug}` | 루트 수정 |
| `DELETE /{slug}` | 루트 삭제 |

#### 4.3 PlaceController

```java
@Tag(name = "Place", description = "네이버/카카오 Place API 프록시 (24h 캐싱)")
```

| Method | Summary |
|--------|---------|
| `GET /search` | 장소 검색 (네이버/카카오) |
| `GET /{provider}/{placeId}` | 장소 상세 조회 |

#### 4.4 MediaController

```java
@Tag(name = "Media", description = "미디어 업로드 (S3 Presigned URL)")
```

| Method | Summary |
|--------|---------|
| `POST /presigned-url` | S3 업로드용 Presigned URL 생성 |
| `DELETE /` | S3 미디어 삭제 |

#### 4.5 SocialController

```java
@Tag(name = "Social", description = "좋아요/저장 토글")
```

| Method | Summary |
|--------|---------|
| `POST /spots/{id}/like` | 스팟 좋아요 토글 |
| `POST /spots/{id}/save` | 스팟 저장 토글 |
| `POST /routes/{id}/like` | 루트 좋아요 토글 |
| `POST /routes/{id}/save` | 루트 저장 토글 |
| `GET /spots/{id}/social` | 스팟 소셜 상태 조회 |
| `GET /routes/{id}/social` | 루트 소셜 상태 조회 |

#### 4.6 CommentController

```java
@Tag(name = "Comment", description = "댓글 CRUD (스팟/루트)")
```

| Method | Summary |
|--------|---------|
| `GET /` | 댓글 목록 조회 (targetType + targetId) |
| `POST /` | 댓글 작성 |
| `PUT /{id}` | 댓글 수정 |
| `DELETE /{id}` | 댓글 삭제 |

#### 4.7 FollowController

```java
@Tag(name = "Follow", description = "사용자 팔로우/팔로워")
```

| Method | Summary |
|--------|---------|
| `POST /users/{userId}/follow` | 팔로우 |
| `DELETE /users/{userId}/follow` | 언팔로우 |
| `GET /users/{userId}/follow/status` | 팔로우 상태 확인 |
| `GET /users/{userId}/followers` | 팔로워 목록 |
| `GET /users/{userId}/following` | 팔로잉 목록 |

#### 4.8 UserController

```java
@Tag(name = "User", description = "사용자 프로필 + 내 컨텐츠")
```

| Method | Summary |
|--------|---------|
| `PUT /me/profile` | 내 프로필 수정 |
| `POST /me/avatar` | 아바타 업로드 |
| `DELETE /me/avatar` | 아바타 삭제 |
| `GET /me/spots` | 내가 생성한 스팟 |
| `GET /me/routes-created` | 내가 생성한 루트 |
| `GET /me/saves` | 내 저장 목록 |
| `GET /{userId}/profile` | 사용자 프로필 조회 |
| `GET /{userId}/likes/spots` | 사용자 좋아요 스팟 |
| `GET /{userId}/saves/routes` | 사용자 저장 루트 |

#### 4.9 UserRouteController

```java
@Tag(name = "UserRoute", description = "루트 복제 + 내 루트 관리")
```

| Method | Summary |
|--------|---------|
| `POST /routes/{spotLineId}/replicate` | 루트 복제 (내 일정으로) |
| `GET /users/me/routes` | 내 루트 목록 |
| `PATCH /users/me/routes/{myRouteId}` | 내 루트 상태 변경 |
| `DELETE /users/me/routes/{myRouteId}` | 내 루트 삭제 |
| `GET /routes/{spotLineId}/variations` | 루트 변형 목록 |

#### 4.10 QrScanController

```java
@Tag(name = "QR", description = "QR 스캔 기록")
```

| Method | Summary |
|--------|---------|
| `POST /qr/{qrId}/scan` | QR 스캔 기록 |

#### 4.11 AnalyticsController

```java
@Tag(name = "Analytics", description = "조회수 + 관리자 통계")
```

| Method | Summary |
|--------|---------|
| `POST /spots/{id}/view` | 스팟 조회수 증가 |
| `POST /routes/{id}/view` | 루트 조회수 증가 |
| `GET /admin/analytics/stats` | 플랫폼 전체 통계 |
| `GET /admin/analytics/popular-spots` | 인기 스팟 순위 |
| `GET /admin/analytics/popular-routes` | 인기 루트 순위 |
| `GET /admin/analytics/daily-trend` | 일별 콘텐츠 추이 |

#### 4.12 ContentReportController

```java
@Tag(name = "Report", description = "콘텐츠 신고 + 관리자 처리")
```

| Method | Summary |
|--------|---------|
| `POST /reports` | 콘텐츠 신고 |
| `GET /admin/reports` | 신고 목록 (관리자) |
| `GET /admin/reports/pending-count` | 미처리 신고 수 |
| `PUT /admin/reports/{id}/resolve` | 신고 처리 |

#### 4.13 PartnerController

```java
@Tag(name = "Partner", description = "QR 파트너 매장 관리 (관리자)")
```

| Method | Summary |
|--------|---------|
| `POST /admin/partners` | 파트너 등록 |
| `GET /admin/partners` | 파트너 목록 |
| `GET /admin/partners/{id}` | 파트너 상세 |
| `PATCH /admin/partners/{id}` | 파트너 수정 |
| `DELETE /admin/partners/{id}` | 파트너 삭제 |
| `POST /admin/partners/{id}/qr-codes` | QR 코드 생성 |
| `GET /admin/partners/{id}/qr-codes` | QR 코드 목록 |
| `PATCH /admin/partners/{id}/qr-codes/{qrCodeId}` | QR 코드 상태 변경 |
| `DELETE /admin/partners/{id}/qr-codes/{qrCodeId}` | QR 코드 삭제 |
| `GET /admin/partners/{id}/analytics` | 파트너 분석 |

#### 4.14 HealthController

```java
@Tag(name = "Health", description = "헬스체크")
```

| Method | Summary |
|--------|---------|
| `GET /health` | 서버 상태 확인 |

---

### Step 5: Request DTO @Schema 어노테이션

주요 Request DTO에 `@Schema` 추가. 필드 설명 + 예시값 + 필수여부 중심.

#### 5.1 CreateSpotRequest

```java
@Schema(description = "스팟 생성 요청")
public class CreateSpotRequest {
    @Schema(description = "업체명", example = "바모스커피 연남점", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "카테고리", example = "CAFE")
    private SpotCategory category;

    @Schema(description = "출처", example = "CREW")
    private SpotSource source;

    @Schema(description = "크루 한줄 추천", example = "연남동 최고의 루프탑 뷰")
    private String crewNote;

    @Schema(description = "도로명 주소", example = "서울 마포구 연남로 123")
    private String address;

    @Schema(description = "위도", example = "37.5665")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    private Double longitude;

    @Schema(description = "지역", example = "연남/연희")
    private String area;

    @Schema(description = "태그 목록", example = "[\"뷰맛집\", \"루프탑\", \"브런치\"]")
    private List<String> tags;

    @Schema(description = "미디어 아이템")
    private List<MediaItemRequest> mediaItems;

    @Schema(description = "작성자명", example = "crew")
    private String creatorName;

    // 나머지 필드는 기존 validation 어노테이션이 springdoc에 의해 자동 감지됨
}
```

#### 5.2 CreateRouteRequest

```java
@Schema(description = "루트 생성 요청")
public class CreateRouteRequest {
    @Schema(description = "루트 제목", example = "연남동 카페 투어")
    private String title;

    @Schema(description = "루트 설명")
    private String description;

    @Schema(description = "테마", example = "CAFE_TOUR")
    private RouteTheme theme;

    @Schema(description = "지역", example = "연남/연희")
    private String area;

    @Schema(description = "스팟 목록 (순서대로)")
    private List<SpotLineSpotRequest> spots;
}
```

#### 5.3 나머지 Request DTO

| DTO | @Schema description |
|-----|---------------------|
| `PresignedUrlRequest` | "S3 업로드 Presigned URL 요청" |
| `CreateCommentRequest` | "댓글 작성 요청" |
| `CreatePartnerRequest` | "파트너 등록 요청" |
| `CreateReportRequest` | "콘텐츠 신고 요청" |
| `UpdateProfileRequest` | "프로필 수정 요청" |
| `UpdateSpotRequest` | "스팟 수정 요청" |
| `UpdateRouteRequest` | "루트 수정 요청" |
| `ReplicateRouteRequest` | "루트 복제 요청" |
| `AvatarUploadRequest` | "아바타 업로드 요청" |
| `MediaItemRequest` | "미디어 아이템 정보" |

각 DTO에 클래스 레벨 `@Schema(description=...)` + 핵심 필드에만 `@Schema(example=...)` 추가.

---

### Step 6: Response DTO @Schema 어노테이션

Response DTO에 클래스 레벨 `@Schema(description=...)` 추가. 필드 레벨은 타입으로 충분히 유추 가능하므로 최소한으로.

| DTO | @Schema description |
|-----|---------------------|
| `SpotDetailResponse` | "스팟 상세 응답" |
| `RouteDetailResponse` | "루트 상세 응답" |
| `RoutePreviewResponse` | "루트 미리보기 응답" |
| `DiscoverResponse` | "QR Discovery 응답" |
| `SocialStatusResponse` | "소셜 상태 응답 (좋아요/저장)" |
| `SocialToggleResponse` | "소셜 토글 결과" |
| `CommentResponse` | "댓글 응답" |
| `UserProfileResponse` | "사용자 프로필 응답" |
| `MyRouteResponse` | "내 루트 응답" |
| `PresignedUrlResponse` | "S3 Presigned URL 응답" |
| `PartnerResponse` | "파트너 상세 응답" |
| `ReportResponse` | "신고 상세 응답" |
| `PlatformStatsResponse` | "플랫폼 통계 응답" |
| `PopularContentResponse` | "인기 콘텐츠 응답" |
| `SimplePageResponse` | "간단 페이지네이션 응답" |
| `SlugResponse` | "Slug 응답 (SSR/sitemap)" |
| `FollowResponse` | "팔로우 결과" |
| `FollowStatusResponse` | "팔로우 상태" |
| `ReplicateRouteResponse` | "루트 복제 결과" |
| `AvatarUploadResponse` | "아바타 업로드 결과" |

---

### Step 7: docs/API_DOCUMENTATION.md 갱신

기존 레거시 Express API 문서를 v2 Spring Boot 기준으로 전면 교체.

**구조**:

```markdown
# Spotline API v2 문서

## 기본 정보
- Base URL: http://localhost:4000/api/v2
- Swagger UI: http://localhost:4000/swagger-ui.html
- 인증: Supabase JWT (Bearer Token)

## API 그룹 요약
(8개 그룹 × 엔드포인트 수 표)

## 인증
(Supabase Auth 설명, JWT 획득 방법)

## 공통 응답 형식
(에러 형식, 페이지네이션 형식)

## 상세 API 참조
→ Swagger UI 참조 안내
```

---

## 3. Verification Checklist

| # | 검증 항목 | 방법 |
|---|----------|------|
| 1 | `./gradlew build` 성공 | 빌드 실행 |
| 2 | `./gradlew bootRun` → `localhost:4000/swagger-ui.html` 접근 | 브라우저 확인 |
| 3 | 16개 컨트롤러 모두 Tag별 그룹 표시 | Swagger UI |
| 4 | JWT 토큰 입력 → 인증 필요 API 호출 성공 | Swagger UI Authorize |
| 5 | Request DTO 필드에 설명/예시 표시 | Swagger UI Schema |
| 6 | SecurityConfig에서 swagger 경로 허용 | curl 확인 |
| 7 | `application-prod.properties`에서 비활성화 | 설정 파일 확인 |

---

## 4. File Change Summary

| Action | File | Description |
|--------|------|-------------|
| MODIFY | `build.gradle` | springdoc-openapi 의존성 추가 |
| CREATE | `config/OpenApiConfig.java` | OpenAPI 설정 + SecurityScheme |
| MODIFY | `application.properties` | springdoc 설정 추가 |
| CREATE | `application-prod.properties` | Swagger UI prod 비활성화 |
| MODIFY | `config/SecurityConfig.java` | swagger 경로 permitAll |
| MODIFY | `controller/SpotController.java` | @Tag + @Operation |
| MODIFY | `controller/RouteController.java` | @Tag + @Operation |
| MODIFY | `controller/PlaceController.java` | @Tag + @Operation |
| MODIFY | `controller/MediaController.java` | @Tag + @Operation |
| MODIFY | `controller/SocialController.java` | @Tag + @Operation |
| MODIFY | `controller/CommentController.java` | @Tag + @Operation |
| MODIFY | `controller/FollowController.java` | @Tag + @Operation |
| MODIFY | `controller/UserController.java` | @Tag + @Operation |
| MODIFY | `controller/UserRouteController.java` | @Tag + @Operation |
| MODIFY | `controller/QrScanController.java` | @Tag + @Operation |
| MODIFY | `controller/AnalyticsController.java` | @Tag + @Operation |
| MODIFY | `controller/ContentReportController.java` | @Tag + @Operation |
| MODIFY | `controller/PartnerController.java` | @Tag + @Operation |
| MODIFY | `controller/HealthController.java` | @Tag + @Operation |
| MODIFY | 17개 Request DTO | @Schema (클래스 + 주요 필드) |
| MODIFY | 20개 Response DTO | @Schema (클래스 레벨) |
| MODIFY | `docs/API_DOCUMENTATION.md` | v2 기준 전면 갱신 |

**총 변경 파일**: ~55개 (1 new, ~54 modify)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-04 | Initial draft | Claude (bkit PDCA) |
