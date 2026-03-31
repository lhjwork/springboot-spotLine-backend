# Phase 1: Data Model + Place API Proxy Caching Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: Spotline Backend v2 (springboot-spotLine-backend)
> **Version**: 2.0.0
> **Analyst**: Claude Code (gap-detector)
> **Date**: 2026-03-19
> **Design Doc**: [phase1-data-model-place-api.design.md](../../02-design/features/phase1-data-model-place-api.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Design document(v0.2, 2026-03-15)와 실제 구현 코드 간의 차이를 식별하여, v0.1 분석(85%) 이후 해결된 항목과 잔여 Gap을 재평가한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/phase1-data-model-place-api.design.md`
- **Implementation Path**: `src/main/java/com/spotline/api/`
- **Test Path**: `src/test/java/com/spotline/api/`
- **Analysis Date**: 2026-03-19
- **Previous Analysis**: v0.1 (2026-03-19, Match Rate: 85%)

### 1.3 v0.1 -> v0.2 Changes Verified

| v0.1 Missing Item | Resolution | Status |
|-------------------|-----------|:------:|
| Bulk API 50-item limit (design.md L443) | `@Size(max=50)` on `SpotController.bulkCreate` L81 | RESOLVED |
| `idx_route_parent` index (design.md L258) | `@Index(name="idx_route_parent", columnList="parent_route_id")` in `Route.java` L20 | RESOLVED |
| Discover `radius` parameter (design.md L498) | `@RequestParam(defaultValue="1.0") double radius` in `SpotController.discover` L31; `SpotService.discover` signature updated to accept `radiusKm` | RESOLVED |

All 3 previously-missing items are now implemented.

---

## 2. Overall Scores

| Category | v0.1 | v0.2 | Status |
|----------|:----:|:----:|:------:|
| API Endpoints | 92% | 100% | ✅ |
| Data Model | 88% | 100% | ✅ |
| Architecture Compliance | 95% | 95% | ✅ |
| Error Handling | 100% | 100% | ✅ |
| Cache Strategy | 100% | 100% | ✅ |
| Security Config | 100% | 100% | ✅ |
| Convention Compliance | 93% | 93% | ✅ |
| Test Coverage | 55% | 89% | ✅ |
| **Overall** | **85%** | **96%** | **✅** |

---

## 3. Gap Analysis (Design vs Implementation)

### 3.1 API Endpoints

| Design Endpoint | Implementation | Status | Notes |
|----------------|---------------|:------:|-------|
| `GET /api/v2/spots/{slug}` | `SpotController.getBySlug` | ✅ Match | PlaceInfo merge |
| `GET /api/v2/spots` | `SpotController.list` | ✅ Match | area, category filter + paging |
| `GET /api/v2/spots/nearby` | `SpotController.nearby` | ✅ Match | lat, lng, radius params |
| `POST /api/v2/spots` | `SpotController.create` | ✅ Match | @Valid + 201 Created |
| `POST /api/v2/spots/bulk` | `SpotController.bulkCreate` | ✅ Match | @Size(max=50) applied |
| `GET /api/v2/routes/{slug}` | `RouteController.getBySlug` | ✅ Match | RouteDetailResponse |
| `GET /api/v2/routes/popular` | `RouteController.popular` | ✅ Match | area, theme filter |
| `POST /api/v2/routes` | `RouteController.create` | ✅ Match | @Valid + 201 Created |
| `GET /api/v2/places/search` | `PlaceController.search` | ✅ Match | query, provider, size |
| `GET /api/v2/places/{provider}/{placeId}` | `PlaceController.detail` | ✅ Match | @Cacheable |
| `GET /api/v2/spots/discover` | `SpotController.discover` | ✅ Match | lat, lng, radius, excludeSpotId |
| `GET /health` | `HealthController.health` | ✅ Match | |
| `PUT /api/v2/spots/{slug}` | `SpotController.update` | Added | Design Step 13 "not implemented" but implemented |
| `DELETE /api/v2/spots/{slug}` | `SpotController.delete` | Added | Design Step 13 "not implemented" but implemented |
| `POST /api/v2/media/presigned-url` | `MediaController` | Added | Media upload (not in design) |
| `DELETE /api/v2/media` | `MediaController` | Added | Media delete (not in design) |

**Endpoint Match Rate**: 12/12 designed endpoints implemented (100%), plus 4 beneficial extras

### 3.2 Data Model

#### 3.2.1 Spot Entity -- All design fields present

All 25 design fields matched: id, slug, title, description, category, source, crewNote, address, latitude, longitude, area, naverPlaceId, kakaoPlaceId, tags(ElementCollection), media(ElementCollection), qrId, qrActive, likesCount, savesCount, viewsCount, creatorType, creatorId, creatorName, isActive, timestamps.

Extra fields (beneficial): `sido`, `sigungu`, `dong`, `blogUrl`, `instagramUrl`, `websiteUrl`, `mediaItems` (OneToMany SpotMedia)

#### 3.2.2 Route Entity -- All design fields present

All fields matched: id, slug, title, description, theme, area, totalDuration, totalDistance, spots(OneToMany RouteSpot), likesCount, savesCount, replicationsCount, completionsCount, creatorType, creatorId, creatorName, parentRoute(ManyToOne self), variations(OneToMany self), isActive, timestamps.

#### 3.2.3 RouteSpot Entity -- All design fields present

All fields matched: id, route(ManyToOne), spot(ManyToOne), spotOrder, suggestedTime, stayDuration, walkingTimeToNext, distanceToNext, transitionNote.

#### 3.2.4 PlaceInfo DTO -- All design fields present

11 fields: provider, placeId, name, address, phone, category, businessHours, rating, reviewCount, photos, url.

#### 3.2.5 Indexes -- All design indexes present

| Design Index | Implementation | Status |
|-------------|---------------|:------:|
| idx_spot_slug | `@Index` on Spot.java L17 | ✅ |
| idx_spot_area | `@Index` on Spot.java L18 | ✅ |
| idx_spot_category | `@Index` on Spot.java L19 | ✅ |
| idx_spot_source | `@Index` on Spot.java L20 | ✅ |
| idx_spot_active | `@Index` on Spot.java L21 | ✅ |
| idx_spot_lat_lng | `@Index` on Spot.java L22 | ✅ |
| idx_route_slug | `@Index` on Route.java L16 | ✅ |
| idx_route_area | `@Index` on Route.java L17 | ✅ |
| idx_route_theme | `@Index` on Route.java L18 | ✅ |
| idx_route_active | `@Index` on Route.java L19 | ✅ |
| idx_route_parent | `@Index` on Route.java L20 | ✅ (NEW - was missing in v0.1) |
| idx_route_spot_route | `@Index` on RouteSpot entity | ✅ |
| idx_route_spot_spot | `@Index` on RouteSpot entity | ✅ |

Extra indexes (beneficial): `idx_spot_sigungu`, `idx_spot_sido`

### 3.3 Error Handling -- Full compliance

| Design Spec | Implementation | Status |
|------------|---------------|:------:|
| 400 Validation (field-level errors) | `MethodArgumentNotValidException` handler + `fieldErrors` | ✅ |
| 404 Not Found (Korean message) | `ResourceNotFoundException` + "...을(를) 찾을 수 없습니다" | ✅ |
| 500 Server Error | General `Exception` handler | ✅ |
| Error format: timestamp, status, error, message, path | `ErrorResponse` class | ✅ |

Extra handlers (beneficial): MissingParam, TypeMismatch, IllegalArgument

### 3.4 Cache Strategy -- Full compliance

| Design Spec | Implementation | Status |
|------------|---------------|:------:|
| Cache Name: `placeInfo` | `CaffeineCacheManager("placeInfo")` in CacheConfig | ✅ |
| Max Size: 2,000 | `@Value("${place.cache.max-size:2000}")` | ✅ |
| TTL: 24h (expireAfterWrite) | `expireAfterWrite(ttlHours, HOURS)` | ✅ |
| Key: `{provider}:{placeId}` | `@Cacheable(key = "#provider + ':' + #placeId")` | ✅ |
| Search: no caching | No @Cacheable on searchNaver/searchKakao | ✅ |
| Detail: cached | @Cacheable on getPlaceDetail | ✅ |

### 3.5 Security / Config -- Full compliance

| Design Spec | Implementation | Status |
|------------|---------------|:------:|
| STATELESS session | `SessionCreationPolicy.STATELESS` | ✅ |
| CSRF disabled | `csrf.disable()` | ✅ |
| CORS config | `CorsConfig` + env-based origins | ✅ |
| @Valid Bean Validation | `@Valid @RequestBody` on POST endpoints | ✅ |
| Bulk API 50-item limit | `@Size(max=50)` on bulkCreate param | ✅ (NEW - was missing in v0.1) |

### 3.6 Discover API -- Full compliance

| Design Field | Implementation | Status |
|-------------|---------------|:------:|
| currentSpot (spot + placeInfo + distanceFromUser) | `CurrentSpotInfo` in DiscoverResponse | ✅ |
| nextSpot (spot + placeInfo + distanceFromCurrent + walkingTime) | `NextSpotInfo` in DiscoverResponse | ✅ |
| nearbySpots (max 6) | `limit(6)` in findNearbyExcluding | ✅ |
| popularRoutes (max 3) | `PageRequest.of(0, 3)` in findPopularRoutes | ✅ |
| area, locationGranted | fields in DiscoverResponse | ✅ |
| radius parameter | `@RequestParam(defaultValue="1.0") double radius` | ✅ (NEW - was missing in v0.1) |
| walkingTime = dist/67 | `dist / 67.0` in SpotService L215 | ✅ |
| Category diversity priority | `category != currentSpot.category` filter | ✅ |
| crewNote preference | crewNote null check in sort comparator | ✅ |
| No-location fallback | `buildFallbackResponse()` | ✅ |

---

## 4. Architecture Compliance (Clean Architecture)

### 4.1 Layer Structure

| Design Layer | Expected Package | Implementation | Status |
|-------------|-----------------|----------------|:------:|
| Presentation | `controller/` | SpotController, RouteController, PlaceController, HealthController, MediaController | ✅ |
| Application | `service/` | SpotService, RouteService, MediaService | ✅ |
| Domain | `domain/entity/`, `domain/enums/`, `domain/repository/` | 4 entities + 4 enums + 3 repos | ✅ |
| DTO | `dto/request/`, `dto/response/` | Request/Response classes | ✅ |
| Infrastructure | `infrastructure/place/`, `infrastructure/s3/` | PlaceApiService, PlaceInfo, S3Service, MediaConstants | ✅ |
| Config | `config/` | SecurityConfig, CorsConfig, CacheConfig, GlobalExceptionHandler, S3Config | ✅ |

### 4.2 Dependency Direction

| Dependency | Expected | Actual | Status |
|-----------|----------|--------|:------:|
| Controller -> Service | Yes | SpotController -> SpotService | ✅ |
| Controller -> DTO | Yes | Request/Response imports | ✅ |
| Service -> Repository | Yes | SpotService -> SpotRepository | ✅ |
| Service -> Infrastructure | Yes | SpotService -> PlaceApiService, S3Service | ✅ |
| Controller !-> Repository | Yes | No direct repo imports in controllers | ✅ |
| Controller !-> Infrastructure | No | PlaceController -> PlaceApiService (direct) | Note |

PlaceController -> PlaceApiService direct injection: Intentional per design Section 8.1. Proxy purpose -- no Service layer needed.

**Architecture Score**: 95%

---

## 5. Convention Compliance

### 5.1 Naming Convention

| Category | Convention | Compliance | Violations |
|----------|-----------|:----------:|------------|
| Entity Class | PascalCase | 100% | - |
| Repository | `{Entity}Repository` | 100% | - |
| Service | `{Domain}Service` | 100% | - |
| Controller | `{Domain}Controller` | 100% | - |
| DTO Request | `Create{Entity}Request` | 100% | - |
| DTO Response | `{Entity}DetailResponse` | 100% | - |
| Enum values | UPPER_SNAKE_CASE | 100% | - |
| API Path | plural nouns | 100% | /spots, /routes, /places |

### 5.2 Environment Variables

| Design Variable | Actual Usage | Status |
|----------------|-------------|:------:|
| `SUPABASE_DB_URL` | application.properties `${SUPABASE_DB_URL}` | ✅ |
| `SUPABASE_DB_USER` | application.properties `${SUPABASE_DB_USER}` | ✅ |
| `SUPABASE_DB_PASSWORD` | application.properties `${SUPABASE_DB_PASSWORD}` | ✅ |
| `NAVER_CLIENT_ID` | `@Value("${place.naver.client-id}")` | ✅ |
| `NAVER_CLIENT_SECRET` | `@Value("${place.naver.client-secret}")` | ✅ |
| `KAKAO_REST_API_KEY` | `@Value("${place.kakao.rest-api-key}")` | ✅ |
| `CORS_ORIGINS` | `@Value("${cors.allowed-origins}")` | ✅ |

### 5.3 Import Order (Java)

Spot across files shows consistent pattern: `java.*` -> `jakarta.*` -> `org.springframework.*` -> `lombok.*` -> `com.spotline.api.*`. Compliance: 100%

**Convention Score**: 93% (Architecture note on PlaceController direct injection is the only delta)

---

## 6. Test Coverage

### 6.1 Test Files

| Test File | Exists | Test Count | v0.1 Status |
|-----------|:------:|:----------:|:-----------:|
| `SpotServiceTest.java` | ✅ | 9 tests (+2 new) | Was 7 |
| `SpotControllerTest.java` | ✅ | 5 tests | Was 5 |
| `SpotlineApiApplicationTests.java` | ✅ | 1 test | Was 1 |
| `RouteServiceTest.java` | ✅ | 5 tests (NEW) | Was 0 |
| `PlaceApiServiceTest.java` | ✅ | 6 tests (NEW) | Was 0 |
| RouteControllerTest.java | -- | 0 | Was 0 |
| PlaceControllerTest.java | -- | 0 | Was 0 |
| MediaServiceTest.java | -- | 0 | Was 0 |

**Total Tests**: 26 (was 13 in v0.1, +100% increase)

### 6.2 Design Test Cases vs Implementation

| Design Test Case (Section 7.2) | Implemented | Test File:Method | Status |
|-------------------------------|:-----------:|------------------|:------:|
| Spot create -> slug auto-gen | Yes | SpotServiceTest.create_generatesSlug | ✅ |
| Spot create -> duplicate slug suffix | Yes (NEW) | SpotServiceTest.create_duplicateSlug_addsSuffix | ✅ |
| Spot detail -> PlaceInfo merge | Yes | SpotServiceTest.getBySlug_returnsSpotWithPlaceInfo | ✅ |
| Spot detail -> Place API fail = null | Yes | SpotServiceTest.getBySlug_placeApiFails_returnsNullPlaceInfo | ✅ |
| Route create -> Spot ref + order | Yes (NEW) | RouteServiceTest.create_setsSpotReferencesAndOrder | ✅ |
| Route create -> totalDuration calc | Yes (NEW) | RouteServiceTest.create_calculatesTotalDuration | ✅ |
| Place search -> proxy normalize | Yes (NEW) | PlaceApiServiceTest.searchNaver_stripsHtmlFromTitle, searchKakao_normalizesResults | ✅ |
| Cache hit -> no API call | No | -- | -- |
| Nearby -> lat/lng range search | Yes (NEW) | SpotServiceTest.findNearby_calculatesLatLngBounds | ✅ |

**Design Test Cases Coverage**: 8/9 implemented (89%)

### 6.3 Additional Tests Beyond Design

| Test | File | Description |
|------|------|-------------|
| SpotServiceTest.getBySlug_notFound_throwsException | SpotServiceTest | 404 handling |
| SpotServiceTest.update_partialUpdate | SpotServiceTest | Partial update |
| SpotServiceTest.delete_softDelete | SpotServiceTest | Soft delete |
| SpotServiceTest.list_filteredByArea | SpotServiceTest | Area filter |
| SpotControllerTest (5 tests) | SpotControllerTest | API layer tests |
| RouteServiceTest.create_invalidSpotId_throwsException | RouteServiceTest | Invalid Spot ID |
| RouteServiceTest.getDetailBySlug_returnsRouteDetail | RouteServiceTest | Route detail |
| RouteServiceTest.getBySlug_notFound_throwsException | RouteServiceTest | 404 handling |
| PlaceApiServiceTest.searchNaver_noApiKey_returnsEmpty | PlaceApiServiceTest | No API key |
| PlaceApiServiceTest.searchKakao_noApiKey_returnsEmpty | PlaceApiServiceTest | No API key |
| PlaceApiServiceTest.searchNaver_apiFails_returnsEmpty | PlaceApiServiceTest | Graceful failure |
| PlaceApiServiceTest.getPlaceDetail_unknownProvider_returnsNull | PlaceApiServiceTest | Unknown provider |

---

## 7. Match Rate Summary

```
+---------------------------------------------+
|  Overall Match Rate: 96%  (was 85%)          |
+---------------------------------------------+
|  v0.1 Missing: 3 items  ->  0 items          |
|  Fully Matched:  52 items  (100% of design)  |
|  Added (impl>design): 11 items (beneficial)  |
|  Remaining gaps: Test (1 case)               |
+---------------------------------------------+
```

### v0.1 -> v0.2 Delta

| Category | v0.1 Score | v0.2 Score | Delta |
|----------|:----------:|:----------:|:-----:|
| API Endpoints | 92% | 100% | +8% |
| Data Model | 88% | 100% | +12% |
| Security Config | 100% | 100% | -- |
| Test Coverage | 55% | 89% | +34% |
| **Overall** | **85%** | **96%** | **+11%** |

---

## 8. Differences Found

### 8.1 Missing Features (Design O, Implementation X)

| # | Item | Design Location | Description | Impact |
|---|------|----------------|-------------|--------|
| 1 | Cache hit test | design.md L628 | "Cache hit -> Place API not called" test case not implemented | Low |

### 8.2 Added Features (Design X, Implementation O)

| # | Item | Implementation Location | Description |
|---|------|------------------------|-------------|
| 1 | Spot update API | `SpotController.update` (PUT) | Design Step 13 marked "not implemented" but done |
| 2 | Spot delete API | `SpotController.delete` (DELETE) | Design Step 13 marked "not implemented" but done |
| 3 | Media Upload API | `MediaController` | Presigned URL + delete |
| 4 | SpotMedia Entity | `domain/entity/SpotMedia.java` | Structured media entity |
| 5 | MediaType Enum | `domain/enums/MediaType.java` | IMAGE, VIDEO |
| 6 | Address decomposition | `sido`, `sigungu`, `dong` fields | Address hierarchy |
| 7 | External links | `blogUrl`, `instagramUrl`, `websiteUrl` | Spot external links |
| 8 | S3 infrastructure | `S3Config`, `S3Service`, `MediaConstants` | AWS S3 integration |
| 9 | UpdateSpotRequest | `dto/request/UpdateSpotRequest.java` | Update DTO |
| 10 | MediaItemRequest | `dto/request/MediaItemRequest.java` | Media request DTO |
| 11 | SpotMediaResponse | `dto/response/SpotMediaResponse.java` | Media response DTO |

### 8.3 Changed Features (Design != Implementation)

| # | Item | Design | Implementation | Impact |
|---|------|--------|----------------|--------|
| 1 | Discover default coords | No location = popular Spot | Seoul center (37.5665, 126.9780) then fallback | Low |
| 2 | Next Spot radius | 1.0km (15min walk) | 1.2km (comment: 15min walk ~1.2km) | Low |
| 3 | Walking speed comment | 67m/min (4km/h) | Code uses 67.0 but comment says ~80m/min | Low |
| 4 | ErrorResponse structure | 5 fields | +fieldErrors for validation | Low (backward compatible) |
| 5 | Spot media structure | ElementCollection only | Legacy + new SpotMedia in parallel | Low (backward compatible) |

---

## 9. Design Document Update Status

Design document Implementation Order (Section 10):

| Step | Design Status | Actual Status | Update Needed |
|------|:------------:|:------------:|:-------------:|
| 1-10 | Done | Implemented | No |
| 11. GlobalExceptionHandler | **Not implemented** | Implemented | Yes |
| 12. RouteDetailResponse | **Not implemented** | Implemented | Yes |
| 13. Spot Update/Delete API | **Not implemented** | Implemented | Yes |
| 14. Place API Detail impl | **Not implemented** | Implemented | Yes |
| 15. Discover API | **Not implemented** | Implemented | Yes |
| 16. Tests | **Not implemented** | Partial (26 unit/API tests) | Partial |

---

## 10. Recommended Actions

### 10.1 Immediate (Design Document Sync)

| # | Priority | Item | Description |
|---|:--------:|------|-------------|
| 1 | Medium | Design Section 10 sync | Update Steps 11-15 to "Done" status |
| 2 | Low | Walking speed comment | Align `SpotService.java` L215 comment with actual value (67m/min, not 80m/min) |

### 10.2 Short-term (1 week)

| # | Priority | Item | Description |
|---|:--------:|------|-------------|
| 1 | Low | Cache hit test | Add integration test verifying @Cacheable prevents duplicate Place API calls |
| 2 | Low | Design doc additions | Add SpotMedia, Media API, address fields, external links, S3 to design doc |

### 10.3 Design Document Updates Needed

Items to reflect in design document to keep it current:

- [ ] Section 10: Update Steps 11-15 to "Done"
- [ ] Section 3: Add SpotMedia entity + MediaType enum
- [ ] Section 3: Add `sido`, `sigungu`, `dong` to Spot schema
- [ ] Section 3: Add `blogUrl`, `instagramUrl`, `websiteUrl` to Spot schema
- [ ] Section 4: Add PUT/DELETE Spot endpoints
- [ ] Section 4: Add Media API endpoints
- [ ] Section 2: Add S3 infrastructure component

---

## 11. Next Steps

- [ ] Sync design document Section 10 (Steps 11-15 as Done)
- [ ] Fix walking speed comment mismatch in SpotService.java
- [ ] Add cache hit integration test (remaining design test case)
- [ ] Proceed to Phase 2 (admin-spotLine) -- Match Rate >= 90% threshold met

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-19 | Initial gap analysis (Match Rate: 85%, 3 missing items) | Claude Code (gap-detector) |
| 0.2 | 2026-03-19 | Re-analysis after fixes: 3 missing items resolved, 13 new tests added. Match Rate: 96% (+11%) | Claude Code (gap-detector) |
