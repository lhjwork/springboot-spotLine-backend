# analytics-dashboard-api Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: Spotline (Backend + Admin + Frontend)
> **Analyst**: AI Assistant
> **Date**: 2026-04-03
> **Design Doc**: [analytics-dashboard-api.design.md](../02-design/features/analytics-dashboard-api.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Design Document Section 11의 14-Item Implementation Checklist를 실제 구현 코드와 1:1 대조하여 Match Rate를 산출한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/analytics-dashboard-api.design.md`
- **Implementation Paths**:
  - Backend: `springboot-spotLine-backend/src/main/java/com/spotline/api/`
  - Admin: `admin-spotLine/src/`
  - Frontend: `front-spotLine/src/`
- **Analysis Date**: 2026-04-03

---

## 2. Implementation Checklist Verification (14 Items)

| # | Item | Repo | Design File | Impl File | Status | Notes |
|:-:|------|------|-------------|-----------|:------:|-------|
| 1 | Route viewsCount field | backend | `Route.java` | `domain/entity/Route.java:67` | ✅ | `@Builder.Default private Integer viewsCount = 0`, placed before `likesCount` as designed |
| 2 | PlatformStatsResponse DTO | backend | `PlatformStatsResponse.java` | `dto/response/PlatformStatsResponse.java` | ✅ | 6 fields match exactly (`totalSpots`, `totalRoutes`, `totalComments`, `totalReports`, `totalSpotViews`, `totalRouteViews`) |
| 3 | PopularContentResponse DTO | backend | `PopularContentResponse.java` | `dto/response/PopularContentResponse.java` | ✅ | 6 fields match exactly (`id`, `slug`, `title`, `label`, `viewsCount`, `commentsCount`) |
| 4 | DailyContentTrendResponse DTO | backend | `DailyContentTrendResponse.java` | `dto/response/DailyContentTrendResponse.java` | ✅ | 3 fields match exactly (`date`, `spotCount`, `routeCount`) |
| 5 | SpotRepository queries | backend | `SpotRepository.java` | `domain/repository/SpotRepository.java:129,133` | ✅ | `sumViewsCountByIsActiveTrue()` and `countDailyCreatedSince()` present |
| 6 | RouteRepository queries | backend | `RouteRepository.java` | `domain/repository/RouteRepository.java:118,121,125` | ✅ | `findTop10ByIsActiveTrueOrderByViewsCountDesc()`, `sumViewsCountByIsActiveTrue()`, `countDailyCreatedSince()` all present |
| 7 | AnalyticsService | backend | `AnalyticsService.java` | `service/AnalyticsService.java` | ✅ | 6 methods match exactly: `getPlatformStats`, `getPopularSpots`, `getPopularRoutes`, `getDailyTrend`, `incrementSpotView`, `incrementRouteView` + `toDateMap` helper |
| 8 | AnalyticsController | backend | `AnalyticsController.java` | `controller/AnalyticsController.java` | ✅ | 6 endpoints match: 4 GET admin + 2 POST view, same paths and signatures |
| 9 | SecurityConfig POST /view permitAll | backend | `SecurityConfig.java` | `config/SecurityConfig.java:41` | ✅ | `.requestMatchers(HttpMethod.POST, "/api/v2/spots/*/view", "/api/v2/routes/*/view").permitAll()` |
| 10 | analyticsAPI.ts | admin | `services/v2/analyticsAPI.ts` | `src/services/v2/analyticsAPI.ts` | ✅ | 3 interfaces + 4 API functions match. Minor: import uses `apiClient` from `"../base/apiClient"` instead of design's `api` from `"../api"` -- functionally equivalent |
| 11 | Dashboard.tsx performance section | admin | `pages/Dashboard.tsx` | `src/pages/Dashboard.tsx` | ✅ | 4 useQuery calls (`platformStats`, `popularSpots`, `popularRoutes`, `dailyTrend`), MetricCards, tables, chart all present |
| 12 | incrementView API functions | frontend | `lib/api.ts` | `src/lib/api.ts:1166,1174` | ✅ | `incrementSpotView` and `incrementRouteView` with fire-and-forget pattern, `timeout: 3000` |
| 13 | ViewTracker component | frontend | `components/common/ViewTracker.tsx` | `src/components/common/ViewTracker.tsx` | ✅ | `"use client"`, useEffect, renders null. Minor: uses static import instead of design's dynamic `import()` -- functionally equivalent |
| 14 | Spot/Route page integration | frontend | `app/spot/[slug]/page.tsx`, `app/route/[slug]/page.tsx` | Both files | ✅ | `<ViewTracker type="spot" id={spot.id} />` and `<ViewTracker type="route" id={route.id} />` |

---

## 3. Differences Found

### 3.1 Minor Differences (Design != Implementation, No Functional Impact)

| # | Item | Design | Implementation | Impact |
|:-:|------|--------|----------------|--------|
| 1 | analyticsAPI.ts import | `import api from "../api"` | `import { apiClient } from "../base/apiClient"` | None -- same axios instance |
| 2 | ViewTracker import style | Dynamic import `import("@/lib/api").then(...)` | Static import `import { incrementSpotView } from "@/lib/api"` | Low -- design intended to exclude from SSR bundle, but component is `"use client"` so static import is already client-only |

### 3.2 Missing Features (Design O, Implementation X)

None.

### 3.3 Added Features (Design X, Implementation O)

None.

---

## 4. Match Rate Summary

```
┌──────────────────────────────────────────────────────┐
│  Implementation Checklist Match Rate: 100% (14/14)   │
├──────────────────────────────────────────────────────┤
│  ✅ Full Match:           12 items (86%)              │
│  ✅ Match (minor diff):    2 items (14%)              │
│  ❌ Not Implemented:       0 items (0%)               │
│  ⚠️ Missing in Design:     0 items (0%)               │
└──────────────────────────────────────────────────────┘
```

---

## 5. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 100% | ✅ |
| API Endpoint Match | 100% | ✅ |
| Data Model Match | 100% | ✅ |
| Architecture Compliance | 100% | ✅ |
| Convention Compliance | 98% | ✅ |
| **Overall** | **100%** | ✅ |

Convention note: 2% deduction for minor import path difference in `analyticsAPI.ts` (uses project's actual `apiClient` base instead of design's simplified `api` import). This is correct behavior -- implementation follows the Admin repo's established pattern.

---

## 6. API Endpoint Verification

| Method | Path | Design | Implementation | Auth | Status |
|--------|------|--------|----------------|------|:------:|
| GET | `/api/v2/admin/analytics/stats` | Section 4.2 | `AnalyticsController:22` | ADMIN | ✅ |
| GET | `/api/v2/admin/analytics/popular-spots` | Section 4.2 | `AnalyticsController:27` | ADMIN | ✅ |
| GET | `/api/v2/admin/analytics/popular-routes` | Section 4.2 | `AnalyticsController:32` | ADMIN | ✅ |
| GET | `/api/v2/admin/analytics/daily-trend` | Section 4.2 | `AnalyticsController:37` | ADMIN | ✅ |
| POST | `/api/v2/spots/{id}/view` | Section 4.2 | `AnalyticsController:45` | permitAll | ✅ |
| POST | `/api/v2/routes/{id}/view` | Section 4.2 | `AnalyticsController:50` | permitAll | ✅ |

---

## 7. Recommended Actions

### 7.1 Optional Improvements (Non-blocking)

| Priority | Item | Description |
|----------|------|-------------|
| Low | ViewTracker dynamic import | Consider switching to dynamic `import()` if client bundle size becomes a concern. Current static import is functionally correct since component is `"use client"`. |
| Low | Design doc update | Update design Section 8.2 to reflect actual `apiClient` import path used in Admin repo. |

### 7.2 Design Document Updates Needed

- [ ] Update `analyticsAPI.ts` import example to match Admin repo pattern (`apiClient` from `"../base/apiClient"`)
- [ ] Update `ViewTracker.tsx` to note that static import is acceptable for `"use client"` components

---

## 8. Conclusion

Implementation matches the design document at **100% Match Rate** across all 14 checklist items spanning 3 repositories. The 2 minor differences found (import paths) are style-level variations with zero functional impact -- the implementation actually follows each repo's established conventions better than the design's simplified examples.

No immediate actions required. Feature is ready for completion report.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-04-03 | Initial analysis -- 14-item checklist verification | AI Assistant |
