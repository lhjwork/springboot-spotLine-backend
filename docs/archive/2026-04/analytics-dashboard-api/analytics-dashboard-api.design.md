# Analytics Dashboard API Design Document

> **Summary**: Admin 대시보드용 플랫폼 분석 API 4종 + Spot/Route 조회수 증가 엔드포인트 + Admin Dashboard 확장 + Frontend 조회수 연동
>
> **Project**: Spotline Backend + Admin + Frontend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft
> **Planning Doc**: [analytics-dashboard-api.plan.md](../../01-plan/features/analytics-dashboard-api.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- 플랫폼 전체 성과 지표를 단일 API로 집계하여 제공
- Spot/Route 조회수를 실제로 추적하는 메커니즘 구현
- 기존 Admin Dashboard(큐레이션 현황)에 성과 섹션을 비파괴적으로 추가
- Frontend 상세 페이지에서 fire-and-forget 방식으로 조회수 증가

### 1.2 Design Principles

- 기존 코드 최소 변경 — 새 파일 추가 위주, 기존 Entity/Repository에 필드/메서드만 추가
- Admin Dashboard 기존 섹션 유지 — 아래에 성과 섹션 append
- 조회수 증가는 UX 무영향 — 실패해도 에러 노출 없음

---

## 2. Architecture

### 2.1 Component Diagram

```
┌──────────────────┐     ┌──────────────────────────────────────┐     ┌────────────┐
│  Admin Dashboard │────▶│  GET /api/v2/admin/analytics/*      │────▶│ PostgreSQL │
│  (React)         │     │  (stats, popular-spots/routes,      │     │ (Supabase) │
│                  │     │   daily-trend)                       │     │            │
└──────────────────┘     └──────────────────────────────────────┘     └────────────┘
                                                                           ▲
┌──────────────────┐     ┌──────────────────────────────────────┐          │
│  Frontend        │────▶│  POST /api/v2/spots/{id}/view       │──────────┘
│  (Next.js SSR)   │     │  POST /api/v2/routes/{id}/view      │
│  Client Component│     │  (permitAll, fire-and-forget)        │
└──────────────────┘     └──────────────────────────────────────┘
```

### 2.2 Data Flow

**Admin Dashboard:**
```
Dashboard mount → 4 parallel API calls → Render cards + table + chart
```

**조회수 증가:**
```
Page mount → useEffect → POST /view (fire-and-forget, no await) → viewsCount++
```

### 2.3 Dependencies

| Component | Depends On | Purpose |
|-----------|-----------|---------|
| AnalyticsController | AnalyticsService | 비즈니스 로직 위임 |
| AnalyticsService | SpotRepository, RouteRepository, CommentRepository, ContentReportRepository | 집계 쿼리 |
| Admin analyticsAPI | Backend /admin/analytics/* | 데이터 페칭 |
| Admin Dashboard | analyticsAPI, Chart.tsx (MetricCard, ChartCard, BarChartComponent) | UI 렌더링 |
| Frontend ViewTracker | Backend /spots/{id}/view, /routes/{id}/view | 조회수 증가 |

---

## 3. Data Model

### 3.1 Route Entity 변경 (viewsCount 추가)

```java
// Route.java — 기존 stats 섹션에 추가
@Builder.Default
@Column(nullable = false)
private Integer viewsCount = 0;
```

**위치**: `likesCount` 바로 위에 추가 (stats 그룹 첫 번째)

### 3.2 DTO 정의

#### PlatformStatsResponse

```java
@Data @Builder
public class PlatformStatsResponse {
    private long totalSpots;
    private long totalRoutes;
    private long totalComments;
    private long totalReports;
    private long totalSpotViews;
    private long totalRouteViews;
}
```

#### PopularContentResponse

```java
@Data @Builder
public class PopularContentResponse {
    private UUID id;
    private String slug;
    private String title;
    private String label;        // area(Spot) 또는 theme(Route)
    private Integer viewsCount;
    private Integer commentsCount;
}
```

**`label` 필드**: Spot은 `area`, Route은 `theme` 값을 매핑. Admin 테이블에서 통일 컬럼으로 표시.

#### DailyContentTrendResponse

```java
@Data @Builder
public class DailyContentTrendResponse {
    private LocalDate date;
    private long spotCount;
    private long routeCount;
}
```

---

## 4. API Specification

### 4.1 Endpoint List

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v2/admin/analytics/stats` | 플랫폼 전체 통계 | ADMIN |
| GET | `/api/v2/admin/analytics/popular-spots` | 인기 Spot Top 10 | ADMIN |
| GET | `/api/v2/admin/analytics/popular-routes` | 인기 Route Top 10 | ADMIN |
| GET | `/api/v2/admin/analytics/daily-trend?days=30` | 일별 콘텐츠 생성 트렌드 | ADMIN |
| POST | `/api/v2/spots/{id}/view` | Spot 조회수 증가 | permitAll |
| POST | `/api/v2/routes/{id}/view` | Route 조회수 증가 | permitAll |

### 4.2 Detailed Specification

#### `GET /api/v2/admin/analytics/stats`

**Response (200):**
```json
{
  "totalSpots": 245,
  "totalRoutes": 18,
  "totalComments": 52,
  "totalReports": 3,
  "totalSpotViews": 1280,
  "totalRouteViews": 340
}
```

#### `GET /api/v2/admin/analytics/popular-spots`

**Response (200):**
```json
[
  {
    "id": "uuid",
    "slug": "cafe-onion-seongsu",
    "title": "카페 어니언 성수",
    "label": "성수",
    "viewsCount": 156,
    "commentsCount": 8
  }
]
```

#### `GET /api/v2/admin/analytics/popular-routes`

**Response (200):**
```json
[
  {
    "id": "uuid",
    "slug": "seongsu-cafe-tour",
    "title": "성수 카페 투어",
    "label": "cafe-tour",
    "viewsCount": 89,
    "commentsCount": 3
  }
]
```

#### `GET /api/v2/admin/analytics/daily-trend?days=30`

**Response (200):**
```json
[
  { "date": "2026-04-01", "spotCount": 5, "routeCount": 1 },
  { "date": "2026-04-02", "spotCount": 3, "routeCount": 0 }
]
```

#### `POST /api/v2/spots/{id}/view`

**Response (200):** (빈 응답 body)

**Error:** 404 Not Found (잘못된 ID)

#### `POST /api/v2/routes/{id}/view`

**Response (200):** (빈 응답 body)

**Error:** 404 Not Found (잘못된 ID)

---

## 5. Repository 쿼리

### 5.1 SpotRepository 추가 메서드

```java
// 이미 존재: findByIsActiveTrueOrderByViewsCountDesc → Top 10용 Pageable 활용
// 추가 필요:
@Query("SELECT COALESCE(SUM(s.viewsCount), 0) FROM Spot s WHERE s.isActive = true")
long sumViewsCountByIsActiveTrue();

@Query("SELECT CAST(s.createdAt AS LocalDate) as date, COUNT(s) as count " +
       "FROM Spot s WHERE s.createdAt >= :since GROUP BY CAST(s.createdAt AS LocalDate)")
List<Object[]> countDailyCreatedSince(@Param("since") LocalDateTime since);
```

### 5.2 RouteRepository 추가 메서드

```java
// Top 10: 기존 findByIsActiveTrueOrderByLikesCountDesc 대신 viewsCount 사용
// viewsCount 추가 후 새 메서드:
List<Route> findTop10ByIsActiveTrueOrderByViewsCountDesc();

@Query("SELECT COALESCE(SUM(r.viewsCount), 0) FROM Route r WHERE r.isActive = true")
long sumViewsCountByIsActiveTrue();

@Query("SELECT CAST(r.createdAt AS LocalDate) as date, COUNT(r) as count " +
       "FROM Route r WHERE r.createdAt >= :since GROUP BY CAST(r.createdAt AS LocalDate)")
List<Object[]> countDailyCreatedSince(@Param("since") LocalDateTime since);
```

### 5.3 기존 Repository 활용

- `CommentRepository.count()` — 전체 댓글 수
- `ContentReportRepository.count()` — 전체 신고 수
- `SpotRepository.countByIsActiveTrue()` — 활성 Spot 수 (이미 존재)
- `RouteRepository.countByIsActiveTrue()` — 활성 Route 수 (이미 존재)

---

## 6. Service Layer

### AnalyticsService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SpotRepository spotRepository;
    private final RouteRepository routeRepository;
    private final CommentRepository commentRepository;
    private final ContentReportRepository contentReportRepository;

    // 1. 플랫폼 통계
    public PlatformStatsResponse getPlatformStats() {
        return PlatformStatsResponse.builder()
            .totalSpots(spotRepository.countByIsActiveTrue())
            .totalRoutes(routeRepository.countByIsActiveTrue())
            .totalComments(commentRepository.count())
            .totalReports(contentReportRepository.count())
            .totalSpotViews(spotRepository.sumViewsCountByIsActiveTrue())
            .totalRouteViews(routeRepository.sumViewsCountByIsActiveTrue())
            .build();
    }

    // 2. 인기 Spot Top 10
    public List<PopularContentResponse> getPopularSpots() {
        return spotRepository.findTop10ByIsActiveTrueOrderByViewsCountDesc()
            .stream()
            .map(s -> PopularContentResponse.builder()
                .id(s.getId()).slug(s.getSlug()).title(s.getTitle())
                .label(s.getArea()).viewsCount(s.getViewsCount())
                .commentsCount(s.getCommentsCount()).build())
            .toList();
    }

    // 3. 인기 Route Top 10
    public List<PopularContentResponse> getPopularRoutes() {
        return routeRepository.findTop10ByIsActiveTrueOrderByViewsCountDesc()
            .stream()
            .map(r -> PopularContentResponse.builder()
                .id(r.getId()).slug(r.getSlug()).title(r.getTitle())
                .label(r.getTheme() != null ? r.getTheme().name() : "")
                .viewsCount(r.getViewsCount())
                .commentsCount(r.getCommentsCount()).build())
            .toList();
    }

    // 4. 일별 트렌드
    public List<DailyContentTrendResponse> getDailyTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<LocalDate, Long> spotMap = toDateMap(spotRepository.countDailyCreatedSince(since));
        Map<LocalDate, Long> routeMap = toDateMap(routeRepository.countDailyCreatedSince(since));

        List<DailyContentTrendResponse> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(DailyContentTrendResponse.builder()
                .date(date)
                .spotCount(spotMap.getOrDefault(date, 0L))
                .routeCount(routeMap.getOrDefault(date, 0L))
                .build());
        }
        return result;
    }

    // 5. Spot 조회수 증가
    @Transactional
    public void incrementSpotView(UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResourceNotFoundException("Spot", spotId.toString()));
        spot.setViewsCount(spot.getViewsCount() + 1);
        spotRepository.save(spot);
    }

    // 6. Route 조회수 증가
    @Transactional
    public void incrementRouteView(UUID spotLineId) {
        Route route = routeRepository.findById(spotLineId)
            .orElseThrow(() -> new ResourceNotFoundException("Route", spotLineId.toString()));
        route.setViewsCount(route.getViewsCount() + 1);
        routeRepository.save(route);
    }

    private Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }
}
```

---

## 7. Controller Layer

### AnalyticsController

```java
@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // Admin endpoints — /api/v2/admin/** → hasRole("ADMIN")
    @GetMapping("/api/v2/admin/analytics/stats")
    public ResponseEntity<PlatformStatsResponse> getStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    @GetMapping("/api/v2/admin/analytics/popular-spots")
    public ResponseEntity<List<PopularContentResponse>> getPopularSpots() {
        return ResponseEntity.ok(analyticsService.getPopularSpots());
    }

    @GetMapping("/api/v2/admin/analytics/popular-routes")
    public ResponseEntity<List<PopularContentResponse>> getPopularRoutes() {
        return ResponseEntity.ok(analyticsService.getPopularRoutes());
    }

    @GetMapping("/api/v2/admin/analytics/daily-trend")
    public ResponseEntity<List<DailyContentTrendResponse>> getDailyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getDailyTrend(days));
    }

    // Public endpoints — POST /api/v2/** → authenticated() in SecurityConfig
    // 조회수 증가는 permitAll이어야 함 → SecurityConfig에 예외 추가 필요
    @PostMapping("/api/v2/spots/{id}/view")
    public ResponseEntity<Void> incrementSpotView(@PathVariable UUID id) {
        analyticsService.incrementSpotView(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v2/routes/{id}/view")
    public ResponseEntity<Void> incrementRouteView(@PathVariable UUID id) {
        analyticsService.incrementRouteView(id);
        return ResponseEntity.ok().build();
    }
}
```

### SecurityConfig 변경

```java
// 기존 POST /api/v2/** → authenticated() 규칙에 예외 추가
// POST /api/v2/spots/*/view, POST /api/v2/routes/*/view → permitAll
.requestMatchers(HttpMethod.POST, "/api/v2/spots/*/view", "/api/v2/routes/*/view").permitAll()
// 이 줄을 기존 POST authenticated() 규칙 **위에** 배치
```

---

## 8. Admin UI Design

### 8.1 Dashboard 확장 레이아웃

```
┌────────────────────────────────────────────────────────┐
│  📊 큐레이션 현황 (기존 유지)                              │
│  [Spot 수] [Route 수] [AreaHeatmap] [CategoryPie]      │
├────────────────────────────────────────────────────────┤
│  📈 플랫폼 성과 (신규)                                    │
│  [총 조회수] [총 댓글] [총 신고] [Spot 조회수]              │
│                                                        │
│  🔥 인기 Spot Top 10        🔥 인기 Route Top 10         │
│  ┌──────────────────────┐  ┌──────────────────────────┐ │
│  │ # Title    Area View │  │ # Title    Theme   View │ │
│  │ 1 카페어니언 성수  156│  │ 1 성수투어 cafe    89  │ │
│  │ 2 ...              │  │ 2 ...                   │ │
│  └──────────────────────┘  └──────────────────────────┘ │
│                                                        │
│  📅 일별 콘텐츠 생성 트렌드 (최근 30일)                     │
│  ┌────────────────────────────────────────────────────┐ │
│  │  BarChart: Spot(blue) + Route(green) stacked bars │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

### 8.2 Admin analyticsAPI.ts

```typescript
// services/v2/analyticsAPI.ts
import api from "../api";

export interface PlatformStats {
  totalSpots: number;
  totalRoutes: number;
  totalComments: number;
  totalReports: number;
  totalSpotViews: number;
  totalRouteViews: number;
}

export interface PopularContent {
  id: string;
  slug: string;
  title: string;
  label: string;
  viewsCount: number;
  commentsCount: number;
}

export interface DailyTrend {
  date: string;
  spotCount: number;
  routeCount: number;
}

export const analyticsAPI = {
  getStats: () => api.get<PlatformStats>("/admin/analytics/stats").then(r => r.data),
  getPopularSpots: () => api.get<PopularContent[]>("/admin/analytics/popular-spots").then(r => r.data),
  getPopularRoutes: () => api.get<PopularContent[]>("/admin/analytics/popular-routes").then(r => r.data),
  getDailyTrend: (days = 30) => api.get<DailyTrend[]>(`/admin/analytics/daily-trend?days=${days}`).then(r => r.data),
};
```

### 8.3 Dashboard.tsx 확장

기존 `Dashboard.tsx` 아래에 성과 섹션 추가:
- `useQuery("platformStats", analyticsAPI.getStats)` — react-query v3 스타일
- `useQuery("popularSpots", analyticsAPI.getPopularSpots)`
- `useQuery("popularRoutes", analyticsAPI.getPopularRoutes)`
- `useQuery("dailyTrend", () => analyticsAPI.getDailyTrend(30))`
- MetricCard 4개: 총 Spot 조회수, 총 Route 조회수, 총 댓글, 총 신고
- 인기 콘텐츠 테이블 2개: Spot/Route 각각 순위, 제목, 지역/테마, 조회수, 댓글수
- BarChartComponent: x=date, spot/route stacked bars

---

## 9. Frontend 연동

### 9.1 api.ts 함수 추가

```typescript
// front-spotLine/src/lib/api.ts
export async function incrementSpotView(spotId: string): Promise<void> {
  try {
    await apiV2.post(`/spots/${spotId}/view`, null, { timeout: 3000 });
  } catch {
    // fire-and-forget: 에러 무시
  }
}

export async function incrementRouteView(spotLineId: string): Promise<void> {
  try {
    await apiV2.post(`/routes/${spotLineId}/view`, null, { timeout: 3000 });
  } catch {
    // fire-and-forget: 에러 무시
  }
}
```

**인증 헤더 불필요** — permitAll 엔드포인트

### 9.2 ViewTracker 클라이언트 컴포넌트

Spot/Route 상세 페이지는 서버 컴포넌트(SSR)이므로, 조회수 증가 호출을 위한 클라이언트 컴포넌트 필요:

```typescript
// front-spotLine/src/components/common/ViewTracker.tsx
"use client";
import { useEffect } from "react";

interface ViewTrackerProps {
  type: "spot" | "route";
  id: string;
}

export default function ViewTracker({ type, id }: ViewTrackerProps) {
  useEffect(() => {
    if (type === "spot") {
      import("@/lib/api").then(({ incrementSpotView }) => incrementSpotView(id));
    } else {
      import("@/lib/api").then(({ incrementRouteView }) => incrementRouteView(id));
    }
  }, [type, id]);

  return null; // 렌더링 없음
}
```

**사용법** (서버 컴포넌트 페이지에서):
```tsx
// spot/[slug]/page.tsx
<ViewTracker type="spot" id={spot.id} />
```

Dynamic import로 API 모듈 로딩 — SSR 번들에 포함 안 됨.

---

## 10. Security Considerations

- [x] Admin analytics API → 기존 `/api/v2/admin/**` hasRole("ADMIN") 규칙 자동 적용
- [x] 조회수 증가 → permitAll (비인증 사용자도 페이지 조회 가능)
- [x] 조회수 중복 방지 없음 (Phase 1 — 단순 증가, 향후 세션 기반 중복 방지)
- [x] SecurityConfig에 POST /view 예외 규칙 추가 필요

---

## 11. Implementation Checklist

| # | Item | Repo | Files | Description |
|:-:|------|------|-------|-------------|
| 1 | Route viewsCount 필드 추가 | backend | `domain/entity/Route.java` | `@Builder.Default private Integer viewsCount = 0` |
| 2 | PlatformStatsResponse DTO | backend | `dto/response/PlatformStatsResponse.java` | 6 필드 |
| 3 | PopularContentResponse DTO | backend | `dto/response/PopularContentResponse.java` | 6 필드 |
| 4 | DailyContentTrendResponse DTO | backend | `dto/response/DailyContentTrendResponse.java` | 3 필드 |
| 5 | SpotRepository 쿼리 추가 | backend | `domain/repository/SpotRepository.java` | sumViewsCount, countDailyCreatedSince |
| 6 | RouteRepository 쿼리 추가 | backend | `domain/repository/RouteRepository.java` | findTop10ByViewsCount, sumViewsCount, countDailyCreatedSince |
| 7 | AnalyticsService | backend | `service/AnalyticsService.java` | 6 메서드 |
| 8 | AnalyticsController | backend | `controller/AnalyticsController.java` | 6 엔드포인트 |
| 9 | SecurityConfig 수정 | backend | `config/SecurityConfig.java` | POST /view permitAll 예외 |
| 10 | analyticsAPI.ts | admin | `services/v2/analyticsAPI.ts` | 4 API 함수 |
| 11 | Dashboard.tsx 확장 | admin | `pages/Dashboard.tsx` | 성과 카드 + 테이블 + 차트 |
| 12 | incrementView API 함수 | frontend | `lib/api.ts` | incrementSpotView, incrementRouteView |
| 13 | ViewTracker 컴포넌트 | frontend | `components/common/ViewTracker.tsx` | useEffect 1회 호출 |
| 14 | Spot/Route 상세 페이지 연동 | frontend | `app/spot/[slug]/page.tsx`, `app/route/[slug]/page.tsx` | ViewTracker 추가 |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
