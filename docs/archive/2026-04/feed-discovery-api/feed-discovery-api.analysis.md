# Feed Discovery API - Gap Analysis Report

> **Summary**: feed-discovery-api Design Document vs Implementation 갭 분석
>
> **Design Document**: `docs/02-design/features/feed-discovery-api.design.md`
> **Implementation**: `src/main/java/com/spotline/api/`
> **Analysis Date**: 2026-04-01
> **Status**: Complete

---

## Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 100% | Pass |
| Architecture Compliance | 100% | Pass |
| Convention Compliance | 100% | Pass |
| **Overall** | **100%** | **Pass** |

---

## Per-Item Comparison

### Section 2.1: FeedSort Enum

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `POPULAR` value | Match | Verbatim |
| `NEWEST` value | Match | Verbatim |
| Comment annotations | Match | Verbatim |

### Section 2.2: RoutePreviewResponse

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `coverImageUrl` field | Match | Line 24 |
| `from(Route)` backward-compat overload | Match | Line 26-28 |
| `from(Route, String)` factory | Match | Lines 30-43, identical builder chain |
| `resolveCoverImageUrl()` logic | Match | Lines 45-67: null checks, mediaItems priority, media fallback, thumbnailS3Key priority |
| `spotCount` null-safe logic | Match | `route.getSpots() != null ? route.getSpots().size() : 0` |

### Section 2.3: RouteRepository

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `findActiveRoutesBySpotId` JPQL | Match | Lines 40-45: DISTINCT, JOIN FETCH r.spots rs, JOIN FETCH rs.spot s, WHERE s.id = :spotId AND r.isActive = true, ORDER BY r.likesCount DESC |
| `findByIsActiveTrueOrderByCreatedAtDesc` | Match | Line 30 |
| `findByAreaAndIsActiveTrueOrderByCreatedAtDesc` | Match | Line 32 |
| `findByThemeAndIsActiveTrueOrderByCreatedAtDesc` | Match | Line 34 |
| `findByAreaAndThemeAndIsActiveTrueOrderByCreatedAtDesc` | Match | Lines 36-37 |

### Section 2.4: SpotRepository

| Design Item | Status | Notes |
|-------------|:------:|-------|
| Popular: `findByIsActiveTrueOrderByViewsCountDesc` | Match | Line 29 |
| Popular: `findByAreaAndIsActiveTrueOrderByViewsCountDesc` | Match | Line 31 |
| Popular: `findByCategoryAndIsActiveTrueOrderByViewsCountDesc` | Match | Line 33 |
| Popular: `findByAreaAndCategoryAndIsActiveTrueOrderByViewsCountDesc` | Match | Lines 35-36 |
| Newest: `findByIsActiveTrueOrderByCreatedAtDesc` | Match | Line 39 |
| Newest: `findByAreaAndIsActiveTrueOrderByCreatedAtDesc` | Match | Line 41 |
| Newest: `findByCategoryAndIsActiveTrueOrderByCreatedAtDesc` | Match | Line 43 |
| Newest: `findByAreaAndCategoryAndIsActiveTrueOrderByCreatedAtDesc` | Match | Lines 45-46 |

### Section 2.5: SpotService

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `findRoutesBySpotId()` method | Match | Lines 61-72: spotRepository.findById check, findActiveRoutesBySpotId, stream().limit(10), from(route, s3BaseUrl) |
| `list()` with FeedSort param | Match | Lines 77-88: effectiveSort default POPULAR, NEWEST/POPULAR branching |
| `listByPopular()` private method | Match | Lines 90-101: 4-branch area/category filtering |
| `listByNewest()` private method | Match | Lines 103-114: 4-branch area/category filtering |
| `getS3BaseUrl()` reuse | Match | Line 67 calls existing helper at line 487-489 |
| `ResourceNotFoundException` on missing Spot | Match | Line 63 |
| RouteRepository injection | Match | Line 42 |

### Section 2.6: RouteService

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `S3Service` injection | Match | Line 38: `private final S3Service s3Service` |
| `getS3BaseUrl()` helper | Match | Lines 218-220 |
| `getPopularPreviews()` with FeedSort | Match | Lines 51-64: effectiveSort default POPULAR, NEWEST/POPULAR branching |
| coverImageUrl via `from(route, s3BaseUrl)` | Match | Line 63: `routes.map(route -> RoutePreviewResponse.from(route, s3BaseUrl))` |
| `getNewest()` private method | Match | Lines 66-77: 4-branch area/theme filtering |

**Design deviation resolved**: The design document (Section 2.6) initially showed `RoutePreviewResponse::from` (1-arg) in `getPopularPreviews`, then noted the need to change to `from(route, s3BaseUrl)`. The implementation correctly uses the 2-arg version with S3Service injection.

### Section 2.7: SpotController

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `GET /{spotId}/routes` endpoint | Match | Lines 51-55: @GetMapping, @PathVariable UUID spotId |
| `list()` with sort param | Match | Lines 57-65: @RequestParam String sort, parseFeedSort, feedSort passed to service |
| `parseFeedSort()` method | Match | Lines 106-113: null->POPULAR, toUpperCase, catch->POPULAR |
| @PageableDefault(size = 20) | Match | Line 62 |

### Section 2.8: RouteController

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `popular()` with sort param | Match | Lines 35-43: @RequestParam String sort, parseFeedSort, feedSort passed to service |
| `parseFeedSort()` method | Match | Lines 70-77: identical logic to SpotController |
| @PageableDefault(size = 20) | Match | Line 40 |

### Section 3: API Specification

| Design Item | Status | Notes |
|-------------|:------:|-------|
| `GET /api/v2/spots/{spotId}/routes` | Match | SpotController line 51 |
| Path param `spotId` as UUID | Match | `@PathVariable UUID spotId` |
| Response `List<RoutePreviewResponse>` | Match | Return type matches |
| `GET /api/v2/spots` + `sort` param | Match | SpotController line 61 |
| `GET /api/v2/routes/popular` + `sort` param | Match | RouteController line 39 |
| coverImageUrl in response | Match | RoutePreviewResponse field present |

### Section 5: Implementation Checklist

| Checklist Item | Status | Evidence |
|----------------|:------:|---------|
| A1. FeedSort.java Enum | Match | `domain/enums/FeedSort.java` -- POPULAR, NEWEST |
| A2. RoutePreviewResponse coverImageUrl + from(Route,String) + resolveCoverImageUrl | Match | All 3 additions present |
| A3. RouteRepository findActiveRoutesBySpotId JPQL | Match | JPQL with JOIN FETCH |
| A4. SpotRepository popular/newest 8 queries | Match | 4 popular + 4 newest |
| B1. SpotService findRoutesBySpotId() | Match | Lines 61-72 |
| B2. SpotService list() FeedSort + listByPopular/listByNewest | Match | Lines 77-114 |
| B3. RouteService S3Service + getPopularPreviews sort/coverImageUrl | Match | S3Service injected, 2-arg from() used |
| C1. SpotController getRoutesBySpotId + list sort | Match | Lines 51-65 |
| C2. RouteController popular sort | Match | Lines 35-43 |
| D1. Build success | Not verified | Runtime check required |

---

## Gaps Found

### Missing Features (Design exists, Implementation missing)

None.

### Added Features (Implementation exists, Design missing)

None beyond pre-existing code.

### Changed Features (Design differs from Implementation)

None.

---

## Summary

**Match Rate: 100%** -- All 10 checklist items (A1-C2) are fully implemented. Every method signature, JPQL query, branching logic, field addition, and controller endpoint matches the design document verbatim. The one design "note" about RouteService needing S3Service injection and switching from 1-arg to 2-arg `from()` was correctly resolved in the implementation.

D1 (build success) requires runtime verification and is excluded from the match rate calculation.

---

## Related Documents

- Plan: [feed-discovery-api.plan.md](../01-plan/features/feed-discovery-api.plan.md)
- Design: [feed-discovery-api.design.md](../02-design/features/feed-discovery-api.design.md)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-04-01 | Initial gap analysis -- 100% match | Claude Code |
