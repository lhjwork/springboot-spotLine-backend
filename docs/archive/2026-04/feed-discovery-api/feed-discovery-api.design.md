# Feed Discovery API Design Document

> **Summary**: 피드/디스커버리 시스템의 백엔드 API 갭을 해소하기 위한 상세 기술 설계. spots/{id}/routes 엔드포인트, 피드 정렬 파라미터, Route 커버 이미지 필드를 구현한다.
>
> **Project**: Spotline (springboot-spotLine-backend)
> **Version**: 1.0.0
> **Author**: Crew
> **Date**: 2026-04-01
> **Status**: Draft
> **Plan Reference**: `docs/01-plan/features/feed-discovery-api.plan.md`

---

## 1. Design Overview

### 1.1 변경 대상 파일 요약

| 파일 | 변경 유형 | 설명 |
|------|-----------|------|
| `domain/enums/FeedSort.java` | **신규** | 피드 정렬 옵션 Enum |
| `domain/repository/RouteRepository.java` | 수정 | `findBySpotId()`, newest 정렬 쿼리 추가 |
| `domain/repository/SpotRepository.java` | 수정 | popular/newest 정렬 쿼리 추가 |
| `dto/response/RoutePreviewResponse.java` | 수정 | `coverImageUrl` 필드 + `from(Route, String)` 오버로드 |
| `service/SpotService.java` | 수정 | `findRoutesBySpotId()`, `list()` sort 처리 |
| `service/RouteService.java` | 수정 | `getPopularPreviews()` sort 처리, coverImageUrl 지원 |
| `controller/SpotController.java` | 수정 | `GET /{spotId}/routes`, `sort` 파라미터 |
| `controller/RouteController.java` | 수정 | `sort` 파라미터 |

### 1.2 의존성 다이어그램

```
SpotController
├── GET /{spotId}/routes  ──→ SpotService.findRoutesBySpotId()
│                                ├── RouteRepository.findBySpotId()  [NEW]
│                                └── RoutePreviewResponse.from(route, s3BaseUrl)  [MODIFIED]
│
├── GET / (list)           ──→ SpotService.list(area, category, sort, pageable)
│                                ├── SpotRepository (popular/newest 쿼리)  [NEW]
│                                └── 기존 로직 유지
│
RouteController
├── GET /popular           ──→ RouteService.getPopularPreviews(area, theme, sort, pageable)
│                                ├── RouteRepository (newest 쿼리)  [NEW]
│                                └── RoutePreviewResponse.from(route, s3BaseUrl)  [MODIFIED]
```

---

## 2. Detailed Design

### 2.1 FeedSort Enum (신규)

**파일**: `src/main/java/com/spotline/api/domain/enums/FeedSort.java`

```java
package com.spotline.api.domain.enums;

public enum FeedSort {
    POPULAR,   // Spot: viewsCount DESC / Route: likesCount DESC
    NEWEST     // createdAt DESC
}
```

- 프론트엔드에서 `?sort=popular` 또는 `?sort=newest`로 전달
- Spring은 `@RequestParam` String을 Enum으로 자동 변환 (대소문자 무관 처리 필요)
- 기본값: `POPULAR` (기존 동작 유지)

---

### 2.2 RoutePreviewResponse 수정 (FR-03)

**파일**: `src/main/java/com/spotline/api/dto/response/RoutePreviewResponse.java`

```java
@Data
@Builder
public class RoutePreviewResponse {
    private UUID id;
    private String slug;
    private String title;
    private RouteTheme theme;
    private String area;
    private Integer totalDuration;
    private Integer totalDistance;
    private Integer spotCount;
    private Integer likesCount;
    private String coverImageUrl;  // NEW

    // 기존 팩토리 (하위 호환 — coverImageUrl = null)
    public static RoutePreviewResponse from(Route route) {
        return from(route, null);
    }

    // 신규 팩토리 (coverImageUrl 포함)
    public static RoutePreviewResponse from(Route route, String s3BaseUrl) {
        return RoutePreviewResponse.builder()
                .id(route.getId())
                .slug(route.getSlug())
                .title(route.getTitle())
                .theme(route.getTheme())
                .area(route.getArea())
                .totalDuration(route.getTotalDuration())
                .totalDistance(route.getTotalDistance())
                .spotCount(route.getSpots() != null ? route.getSpots().size() : 0)
                .likesCount(route.getLikesCount())
                .coverImageUrl(resolveCoverImageUrl(route, s3BaseUrl))
                .build();
    }

    private static String resolveCoverImageUrl(Route route, String s3BaseUrl) {
        if (s3BaseUrl == null || route.getSpots() == null || route.getSpots().isEmpty()) {
            return null;
        }
        // 첫 번째 Spot (spotOrder 기준 정렬됨)
        Spot firstSpot = route.getSpots().get(0).getSpot();
        if (firstSpot == null) return null;

        // 1) mediaItems (structured) 우선
        if (firstSpot.getMediaItems() != null && !firstSpot.getMediaItems().isEmpty()) {
            SpotMedia firstMedia = firstSpot.getMediaItems().get(0);
            String key = firstMedia.getThumbnailS3Key() != null
                    ? firstMedia.getThumbnailS3Key()
                    : firstMedia.getS3Key();
            return s3BaseUrl + "/" + key;
        }

        // 2) media (legacy String list) 폴백
        if (firstSpot.getMedia() != null && !firstSpot.getMedia().isEmpty()) {
            return s3BaseUrl + "/" + firstSpot.getMedia().get(0);
        }

        return null;
    }
}
```

**설계 결정**:
- `resolveCoverImageUrl`은 static private 메서드로 DTO 내에 위치 (Service 레이어 오염 방지)
- thumbnail 우선 → 원본 폴백 (성능/대역폭 최적화)
- Route→RouteSpot→Spot→SpotMedia 체인이므로 Fetch Join 필수 (N+1 방지)

---

### 2.3 RouteRepository 수정

**파일**: `src/main/java/com/spotline/api/domain/repository/RouteRepository.java`

```java
// ---- FR-01: Spot이 포함된 Route 조회 ----
@Query("SELECT DISTINCT r FROM Route r " +
       "JOIN FETCH r.spots rs " +
       "JOIN FETCH rs.spot s " +
       "WHERE s.id = :spotId AND r.isActive = true " +
       "ORDER BY r.likesCount DESC")
List<Route> findActiveRoutesBySpotId(@Param("spotId") UUID spotId);

// ---- FR-04: newest 정렬 쿼리 ----
Page<Route> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
Page<Route> findByAreaAndIsActiveTrueOrderByCreatedAtDesc(String area, Pageable pageable);
Page<Route> findByThemeAndIsActiveTrueOrderByCreatedAtDesc(RouteTheme theme, Pageable pageable);
Page<Route> findByAreaAndThemeAndIsActiveTrueOrderByCreatedAtDesc(
        String area, RouteTheme theme, Pageable pageable);
```

**N+1 방지 전략 (FR-01)**:
- `JOIN FETCH r.spots rs JOIN FETCH rs.spot s`로 Route→RouteSpot→Spot을 한 번에 로드
- coverImageUrl 계산을 위해 Spot.media/mediaItems도 필요하지만, `@ElementCollection(EAGER)` + `@OneToMany(LAZY)`이므로:
  - `media` (legacy): EAGER이므로 자동 로드
  - `mediaItems` (structured): LAZY이므로 별도 처리 필요
- **결론**: `findActiveRoutesBySpotId` 결과의 각 Route에 대해 Spot의 mediaItems를 접근하면 추가 쿼리 발생 → Route 수가 최대 10개이므로 허용 범위. 최적화가 필요하면 `@EntityGraph` 또는 별도 쿼리로 확장 가능

**Paginated 쿼리 (FR-04)**:
- Spring Data JPA 메서드 네이밍 규칙으로 자동 생성
- 기존 `OrderByLikesCountDesc` 패턴과 동일하게 `OrderByCreatedAtDesc` 추가

---

### 2.4 SpotRepository 수정 (FR-02)

**파일**: `src/main/java/com/spotline/api/domain/repository/SpotRepository.java`

```java
// ---- FR-02: popular 정렬 (viewsCount DESC) ----
Page<Spot> findByIsActiveTrueOrderByViewsCountDesc(Pageable pageable);
Page<Spot> findByAreaAndIsActiveTrueOrderByViewsCountDesc(String area, Pageable pageable);
Page<Spot> findByCategoryAndIsActiveTrueOrderByViewsCountDesc(SpotCategory category, Pageable pageable);
Page<Spot> findByAreaAndCategoryAndIsActiveTrueOrderByViewsCountDesc(
        String area, SpotCategory category, Pageable pageable);

// ---- FR-02: newest 정렬 (createdAt DESC) ----
Page<Spot> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
Page<Spot> findByAreaAndIsActiveTrueOrderByCreatedAtDesc(String area, Pageable pageable);
Page<Spot> findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(SpotCategory category, Pageable pageable);
Page<Spot> findByAreaAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(
        String area, SpotCategory category, Pageable pageable);
```

**인덱스 고려**:
- `viewsCount`에 인덱스 없음 → 초기 데이터 300개 수준이므로 OK
- 데이터 1,000개 이상 시 `idx_spot_views_count` 추가 검토

---

### 2.5 SpotService 수정

**파일**: `src/main/java/com/spotline/api/service/SpotService.java`

#### 2.5.1 findRoutesBySpotId() (신규 — FR-01)

```java
/**
 * 특정 Spot이 포함된 활성 Route 프리뷰 목록 반환 (최대 10개)
 */
public List<RoutePreviewResponse> findRoutesBySpotId(UUID spotId) {
    // 1. Spot 존재 확인
    spotRepository.findById(spotId)
            .orElseThrow(() -> new ResourceNotFoundException("Spot", spotId.toString()));

    // 2. Route 조회 (Fetch Join으로 RouteSpot + Spot 포함)
    List<Route> routes = routeRepository.findActiveRoutesBySpotId(spotId);

    // 3. 최대 10개 제한 + coverImageUrl 포함 변환
    String s3BaseUrl = getS3BaseUrl();
    return routes.stream()
            .limit(10)
            .map(route -> RoutePreviewResponse.from(route, s3BaseUrl))
            .toList();
}
```

**설계 포인트**:
- Spot 존재 확인 실패 시 404 반환 (ResourceNotFoundException)
- `limit(10)`을 Java 스트림에서 처리 (JPQL에서 `TOP 10` 보다 유연)
- s3BaseUrl은 SpotService에 이미 `getS3BaseUrl()` 헬퍼 존재 → 재사용

#### 2.5.2 list() sort 파라미터 추가 (FR-02)

```java
/**
 * Spot 목록 조회 (정렬 지원)
 */
public Page<SpotDetailResponse> list(String area, String category, FeedSort sort, Pageable pageable) {
    Page<Spot> spots;
    FeedSort effectiveSort = (sort != null) ? sort : FeedSort.POPULAR;

    if (effectiveSort == FeedSort.NEWEST) {
        spots = listByNewest(area, category, pageable);
    } else {
        spots = listByPopular(area, category, pageable);
    }

    return spots.map(spot -> SpotDetailResponse.from(spot, null));
}

private Page<Spot> listByPopular(String area, String category, Pageable pageable) {
    if (area != null && category != null) {
        return spotRepository.findByAreaAndCategoryAndIsActiveTrueOrderByViewsCountDesc(
                area, SpotCategory.valueOf(category.toUpperCase()), pageable);
    } else if (area != null) {
        return spotRepository.findByAreaAndIsActiveTrueOrderByViewsCountDesc(area, pageable);
    } else if (category != null) {
        return spotRepository.findByCategoryAndIsActiveTrueOrderByViewsCountDesc(
                SpotCategory.valueOf(category.toUpperCase()), pageable);
    }
    return spotRepository.findByIsActiveTrueOrderByViewsCountDesc(pageable);
}

private Page<Spot> listByNewest(String area, String category, Pageable pageable) {
    if (area != null && category != null) {
        return spotRepository.findByAreaAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(
                area, SpotCategory.valueOf(category.toUpperCase()), pageable);
    } else if (area != null) {
        return spotRepository.findByAreaAndIsActiveTrueOrderByCreatedAtDesc(area, pageable);
    } else if (category != null) {
        return spotRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
                SpotCategory.valueOf(category.toUpperCase()), pageable);
    }
    return spotRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
}
```

**하위 호환**:
- `sort` 파라미터 없으면 `POPULAR` (viewsCount DESC) 적용
- 기존 `list(area, category, pageable)` 시그니처는 Controller에서만 호출하므로 직접 수정

---

### 2.6 RouteService 수정 (FR-04)

**파일**: `src/main/java/com/spotline/api/service/RouteService.java`

```java
public Page<RoutePreviewResponse> getPopularPreviews(
        String area, String theme, FeedSort sort, Pageable pageable) {
    FeedSort effectiveSort = (sort != null) ? sort : FeedSort.POPULAR;
    Page<Route> routes;

    if (effectiveSort == FeedSort.NEWEST) {
        routes = getNewest(area, theme, pageable);
    } else {
        routes = getPopular(area, theme, pageable);
    }

    return routes.map(RoutePreviewResponse::from);
}

private Page<Route> getNewest(String area, String theme, Pageable pageable) {
    if (area != null && theme != null) {
        return routeRepository.findByAreaAndThemeAndIsActiveTrueOrderByCreatedAtDesc(
                area, RouteTheme.valueOf(theme.toUpperCase()), pageable);
    } else if (area != null) {
        return routeRepository.findByAreaAndIsActiveTrueOrderByCreatedAtDesc(area, pageable);
    } else if (theme != null) {
        return routeRepository.findByThemeAndIsActiveTrueOrderByCreatedAtDesc(
                RouteTheme.valueOf(theme.toUpperCase()), pageable);
    }
    return routeRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
}
```

**coverImageUrl 주의사항**:
- `getPopularPreviews`에서 `RoutePreviewResponse::from` (1인자 버전) 사용 중
- coverImageUrl을 포함하려면 `from(route, s3BaseUrl)` 호출 필요
- RouteService에 S3Service 의존성 추가가 필요하지만, 현재 RouteService에는 S3Service가 없음
- **접근법**: RouteService에 `S3Service` 주입 + `getS3BaseUrl()` 헬퍼 추가

```java
// RouteService에 추가
private final S3Service s3Service;

private String getS3BaseUrl() {
    return s3Service.getPublicUrl("").replaceAll("/$", "");
}
```

그리고 `getPopularPreviews`에서:
```java
String s3BaseUrl = getS3BaseUrl();
return routes.map(route -> RoutePreviewResponse.from(route, s3BaseUrl));
```

---

### 2.7 SpotController 수정

**파일**: `src/main/java/com/spotline/api/controller/SpotController.java`

```java
// ---- FR-01: Spot이 포함된 Route 목록 ----
@GetMapping("/{spotId}/routes")
public ResponseEntity<List<RoutePreviewResponse>> getRoutesBySpotId(
        @PathVariable UUID spotId) {
    return ResponseEntity.ok(spotService.findRoutesBySpotId(spotId));
}

// ---- FR-02: list에 sort 파라미터 추가 ----
@GetMapping
public ResponseEntity<Page<SpotDetailResponse>> list(
        @RequestParam(required = false) String area,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String sort,
        @PageableDefault(size = 20) Pageable pageable) {
    FeedSort feedSort = parseFeedSort(sort);
    return ResponseEntity.ok(spotService.list(area, category, feedSort, pageable));
}

private FeedSort parseFeedSort(String sort) {
    if (sort == null) return FeedSort.POPULAR;
    try {
        return FeedSort.valueOf(sort.toUpperCase());
    } catch (IllegalArgumentException e) {
        return FeedSort.POPULAR; // 잘못된 값이면 기본값
    }
}
```

**URL 패턴 충돌 방지**:
- `GET /api/v2/spots/{spotId}/routes` vs `GET /api/v2/spots/{slug}` → spotId는 UUID, slug는 String
- `{spotId}`에 `UUID` 타입 명시하면 Spring이 자동으로 UUID 형태만 매칭
- Non-UUID 형태의 path는 `{slug}` 엔드포인트로 라우팅 → 충돌 없음

---

### 2.8 RouteController 수정

**파일**: `src/main/java/com/spotline/api/controller/RouteController.java`

```java
@GetMapping("/popular")
public ResponseEntity<Page<RoutePreviewResponse>> popular(
        @RequestParam(required = false) String area,
        @RequestParam(required = false) String theme,
        @RequestParam(required = false) String sort,
        @PageableDefault(size = 20) Pageable pageable) {
    FeedSort feedSort = parseFeedSort(sort);
    return ResponseEntity.ok(routeService.getPopularPreviews(area, theme, feedSort, pageable));
}

private FeedSort parseFeedSort(String sort) {
    if (sort == null) return FeedSort.POPULAR;
    try {
        return FeedSort.valueOf(sort.toUpperCase());
    } catch (IllegalArgumentException e) {
        return FeedSort.POPULAR;
    }
}
```

---

## 3. API Specification

### 3.1 `GET /api/v2/spots/{spotId}/routes` (신규)

| Item | Detail |
|------|--------|
| Method | GET |
| Path | `/api/v2/spots/{spotId}/routes` |
| Auth | 불필요 (공개 API) |
| Path Param | `spotId` (UUID) |
| Response | `RoutePreviewResponse[]` |
| Max Items | 10 |
| Sort | likesCount DESC (고정) |

**Response 예시**:
```json
[
  {
    "id": "uuid-...",
    "slug": "seongsu-cafe-tour",
    "title": "성수 카페 투어",
    "theme": "CAFE_TOUR",
    "area": "성수",
    "totalDuration": 180,
    "totalDistance": 2400,
    "spotCount": 4,
    "likesCount": 12,
    "coverImageUrl": "https://cdn.spotline.kr/spots/media/abc123.jpg"
  }
]
```

**에러 응답**:
| Status | Condition |
|--------|-----------|
| 200 | 성공 (빈 배열 가능) |
| 404 | spotId에 해당하는 Spot 없음 |
| 400 | spotId가 UUID 형식이 아닌 경우 |

### 3.2 `GET /api/v2/spots` (수정)

| Item | 기존 | 변경 후 |
|------|------|---------|
| Query Params | `area`, `category`, `page`, `size` | + `sort` (popular/newest, default: popular) |
| 기본 정렬 | DB 삽입 순서 (무정렬) | viewsCount DESC (popular) |

### 3.3 `GET /api/v2/routes/popular` (수정)

| Item | 기존 | 변경 후 |
|------|------|---------|
| Query Params | `area`, `theme`, `page`, `size` | + `sort` (popular/newest, default: popular) |
| Response 변경 | coverImageUrl 없음 | `coverImageUrl` 필드 추가 (nullable) |

---

## 4. Data Flow

### 4.1 spots/{spotId}/routes 흐름

```
Client
  │ GET /api/v2/spots/{spotId}/routes
  ▼
SpotController.getRoutesBySpotId(spotId)
  │
  ▼
SpotService.findRoutesBySpotId(spotId)
  │
  ├── spotRepository.findById(spotId) → 404 if not found
  │
  ├── routeRepository.findActiveRoutesBySpotId(spotId)
  │   │ SQL: SELECT DISTINCT r.* FROM routes r
  │   │      JOIN route_spots rs ON rs.route_id = r.id
  │   │      JOIN spots s ON rs.spot_id = s.id
  │   │      WHERE s.id = ? AND r.is_active = true
  │   │      ORDER BY r.likes_count DESC
  │   ▼
  │   List<Route> (with RouteSpot + Spot eagerly loaded)
  │
  ├── s3Service.getPublicUrl("") → s3BaseUrl
  │
  └── routes.stream().limit(10)
      .map(r → RoutePreviewResponse.from(r, s3BaseUrl))
      │
      ├── resolveCoverImageUrl(route, s3BaseUrl)
      │   ├── route.spots[0].spot.mediaItems[0].thumbnailS3Key → URL
      │   └── fallback: route.spots[0].spot.media[0] → URL
      │
      └── List<RoutePreviewResponse>
```

### 4.2 프론트엔드 연동 (변경 없음)

```
front-spotLine/src/lib/api.ts:314
  fetchSpotRoutes(spotId) → GET /api/v2/spots/${spotId}/routes
  → RoutePreview[] (id, slug, title, theme, area, ...)

front-spotLine/src/app/spot/[slug]/page.tsx:69
  const routes = await fetchSpotRoutes(spot.id);
  → "이 Spot이 포함된 Route" 섹션 렌더링
```

---

## 5. Implementation Checklist

구현 순서 (의존성 기반):

### Phase A: 기반 코드 (의존성 없음)

- [ ] **A1**. `FeedSort.java` Enum 생성 (`domain/enums/`)
- [ ] **A2**. `RoutePreviewResponse.java` 수정: `coverImageUrl` 필드 + `from(Route, String)` + `resolveCoverImageUrl()`
- [ ] **A3**. `RouteRepository.java` 수정: `findActiveRoutesBySpotId()` JPQL 추가
- [ ] **A4**. `SpotRepository.java` 수정: popular/newest 정렬 쿼리 8개 추가

### Phase B: 서비스 로직 (Phase A 의존)

- [ ] **B1**. `SpotService.java` 수정: `findRoutesBySpotId()` 메서드 추가
- [ ] **B2**. `SpotService.java` 수정: `list()` 메서드에 `FeedSort` 파라미터 추가 + `listByPopular()`/`listByNewest()` private 메서드
- [ ] **B3**. `RouteService.java` 수정: `S3Service` 주입 + `getPopularPreviews()` sort/coverImageUrl 지원

### Phase C: 컨트롤러 (Phase B 의존)

- [ ] **C1**. `SpotController.java` 수정: `getRoutesBySpotId()` 엔드포인트 + `list()` sort 파라미터
- [ ] **C2**. `RouteController.java` 수정: `popular()` sort 파라미터

### Phase D: 검증

- [ ] **D1**. `./gradlew build` 성공 확인
- [ ] **D2**. 프론트엔드 연동 테스트 (Spot 상세 → Route 섹션)
- [ ] **D3**. 피드 정렬 테스트 (`?sort=popular`, `?sort=newest`)

---

## 6. Edge Cases & Error Handling

| 시나리오 | 처리 |
|----------|------|
| spotId에 연결된 Route가 0개 | 빈 배열 `[]` 반환 (200 OK) |
| spotId가 존재하지 않는 UUID | 404 ResourceNotFoundException |
| spotId가 UUID 형식이 아님 | 400 MethodArgumentTypeMismatchException (Spring 자동 처리) |
| sort 파라미터에 잘못된 값 (예: `?sort=abc`) | 기본값 `POPULAR` 적용 (에러 아닌 graceful) |
| Route의 첫 Spot에 미디어 없음 | coverImageUrl = null |
| Route의 spots 리스트 비어있음 | coverImageUrl = null, spotCount = 0 |
| category 파라미터에 잘못된 Enum 값 | 400 IllegalArgumentException (기존 동작 유지) |

---

## 7. Performance Considerations

| 항목 | 예상 쿼리 수 | 최적화 |
|------|------------|--------|
| `spots/{spotId}/routes` | 2 (Spot 확인 + Route JOIN) + N (mediaItems LAZY) | Route 최대 10개 → N 최대 10 |
| `spots?sort=popular` | 1 (Paginated) | 기존과 동일, ORDER BY 추가만 |
| `routes/popular` (with coverImageUrl) | 1 (Paginated) + N (Route별 Spot media) | Page size 20 → N 최대 20 |

**성능 임계점**:
- 현재 데이터 규모 (300 Spots, ~20 Routes) → 문제 없음
- 1,000+ 데이터 시 `viewsCount` 인덱스 추가 고려
- coverImageUrl N+1은 Page size(20) 범위 내이므로 당분간 허용

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-04-01 | Initial design — Feed Discovery API 상세 기술 설계 | Claude Code |
