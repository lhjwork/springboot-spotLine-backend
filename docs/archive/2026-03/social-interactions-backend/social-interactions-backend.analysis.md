# social-interactions-backend Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: springboot-spotLine-backend
> **Analyst**: gap-detector
> **Date**: 2026-03-31
> **Design Doc**: [social-interactions-backend.design.md](../02-design/features/social-interactions-backend.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Design document (Section 7: 20 implementation items) vs actual Java source code comparison for the social-interactions-backend feature.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/social-interactions-backend.design.md`
- **Implementation Path**: `src/main/java/com/spotline/api/`
- **Analysis Date**: 2026-03-31

---

## 2. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match | 97% | ✅ |
| Architecture Compliance | 100% | ✅ |
| Convention Compliance | 100% | ✅ |
| **Overall** | **97%** | ✅ |

---

## 3. Checklist Item-by-Item Comparison (20 items)

### Item 1: AuthUtil (`security/AuthUtil.java`)

| Aspect | Status | Notes |
|--------|:------:|-------|
| File exists | ✅ | `security/AuthUtil.java` |
| `@Component` | ✅ | |
| `getCurrentUserId()` | ✅ | Exact match to design |
| `requireUserId()` | ✅ | Exact match (UNAUTHORIZED + Korean message) |

**Bonus**: Implementation adds `getCurrentEmail()` method not in design -- beneficial for UserSyncService email extraction.

**Verdict**: ✅ Full match (+1 beneficial extra)

---

### Item 2: User Entity + Repository

| Aspect | Status | Notes |
|--------|:------:|-------|
| `@Entity @Table(name="users")` | ✅ | |
| `@Id` (no `@GeneratedValue`) | ✅ | Supabase UUID |
| Fields: email, nickname, avatar, bio, instagramId | ✅ | All 5 fields match |
| followersCount/followingCount `@Builder.Default = 0` | ✅ | |
| `@CreationTimestamp` / `@UpdateTimestamp` | ✅ | |
| `UserRepository.findByEmail()` | ✅ | |

**Verdict**: ✅ Full match

---

### Item 3: UserSyncService (`service/UserSyncService.java`)

| Aspect | Status | Notes |
|--------|:------:|-------|
| `@Service @RequiredArgsConstructor` | ✅ | |
| `getOrCreateUser(userId, email)` | ✅ | |
| Lazy sync pattern (findById or save) | ✅ | |
| Nickname default = email.split("@")[0] | ✅ | |

**Improvement over design**: Adds null-safe email handling (`email != null ? email : userId + "@unknown"`), preventing NPE when email is not available from JWT.

**Verdict**: ✅ Full match (+1 defensive improvement)

---

### Items 4-7: Social Entities (SpotLike, SpotSave, RouteLike, RouteSave)

| Entity | Table | UniqueConstraint | GenerationType.UUID | ManyToOne LAZY | CreationTimestamp | Status |
|--------|-------|:----------------:|:-------------------:|:--------------:|:-----------------:|:------:|
| SpotLike | spot_likes | ✅ (user_id, spot_id) | ✅ | ✅ Spot | ✅ | ✅ |
| SpotSave | spot_saves | ✅ (user_id, spot_id) | ✅ | ✅ Spot | ✅ | ✅ |
| RouteLike | route_likes | ✅ (user_id, route_id) | ✅ | ✅ Route | ✅ | ✅ |
| RouteSave | route_saves | ✅ (user_id, route_id) | ✅ | ✅ Route | ✅ | ✅ |

**Verdict**: ✅ All 4 entities exact match

---

### Item 8: UserFollow Entity + Repository

| Aspect | Status | Notes |
|--------|:------:|-------|
| `@Table(name="user_follows")` | ✅ | |
| UniqueConstraint (follower_id, following_id) | ✅ | |
| UUID id with GenerationType.UUID | ✅ | |
| String followerId, followingId | ✅ | |
| Repository: 4 query methods | ✅ | findByFollowerIdAndFollowingId, existsBy..., findByFollowingId, findByFollowerId |

**Verdict**: ✅ Full match

---

### Item 9: UserRoute Entity + Repository

| Aspect | Status | Notes |
|--------|:------:|-------|
| `@Table(name="user_routes")` with `@Index` | ✅ | idx_user_routes_user_id |
| ManyToOne Route (LAZY) | ✅ | |
| scheduledDate (String) | ✅ | |
| status `@Builder.Default = "scheduled"` | ✅ | |
| completedAt (LocalDateTime) | ✅ | |
| Repository: 2 query methods | ✅ | findByUserId, findByUserIdAndStatus |

**Verdict**: ✅ Full match

---

### Item 10: Response DTOs (7)

| DTO | Exists | Fields Match | Status |
|-----|:------:|:------------:|:------:|
| SocialToggleResponse | ✅ | ✅ liked, saved, likesCount, savesCount | ✅ |
| SocialStatusResponse | ✅ | ⚠️ | See below |
| FollowResponse | ✅ | ✅ followed, followersCount | ✅ |
| FollowStatusResponse | ✅ | ⚠️ | See below |
| UserProfileResponse | ✅ | ✅ All fields + UserStatsResponse inner class | ✅ |
| MyRouteResponse | ✅ | ✅ All 11 fields | ✅ |
| ReplicateRouteResponse | ✅ | ✅ myRoute + replicationsCount | ✅ |
| SimplePageResponse | ✅ | ✅ items + hasMore + static from() | ✅ |

**SocialStatusResponse deviation**: Design uses `boolean isLiked / isSaved` field names directly. Implementation uses `boolean liked / boolean saved` with `@JsonProperty("isLiked")` / `@JsonProperty("isSaved")` annotations. The JSON output is identical to what front-end expects (`{ "isLiked": true, "isSaved": false }`). This is a correct Jackson approach to avoid the `is` prefix getter issue.

**FollowStatusResponse deviation**: Same pattern -- field `boolean following` with `@JsonProperty("isFollowing")`. JSON output matches design spec.

**MyRouteResponse improvement**: Adds null-safety checks (`route.getSpots() != null`, `ur.getCompletedAt() != null`, `ur.getCreatedAt() != null`). Also improves parentRouteId by checking `route.getParentRoute()` instead of always using `route.getId()`.

**Verdict**: ✅ 7/7 DTOs match (2 use @JsonProperty for correct serialization -- functionally equivalent)

---

### Item 11: Request DTOs (2)

| DTO | Fields | Status |
|-----|--------|:------:|
| ReplicateRouteRequest | scheduledDate (String) | ✅ |
| UpdateMyRouteStatusRequest | status (String) | ✅ |

**Verdict**: ✅ Full match

---

### Item 12: SocialService

| Method | Design | Implementation | Status |
|--------|--------|---------------|:------:|
| toggleSpotLike | ✅ | ✅ | ✅ |
| toggleSpotSave | ✅ | ✅ | ✅ |
| toggleRouteLike | ✅ | ✅ | ✅ |
| toggleRouteSave | ✅ | ✅ | ✅ |
| getSpotSocialStatus | ✅ | ✅ | ✅ |
| getRouteSocialStatus | ✅ | ✅ | ✅ |
| Dependencies (6 repos) | ✅ | ✅ | ✅ |
| `@Transactional` | ✅ | ✅ | ✅ |

**Improvement over design**: Implementation adds `Math.max(0, count - 1)` to prevent negative counts on unlike/unsave. Design only had this in `toggleSpotLike` decrement (`spot.getLikesCount() - 1` without Math.max). Implementation adds `@Transactional(readOnly = true)` on read-only methods.

**Verdict**: ✅ Full match (+defensive improvements)

---

### Item 13: SocialController (6 endpoints)

| Endpoint | Method | Design | Implementation | Status |
|----------|--------|--------|---------------|:------:|
| /api/v2/spots/{id}/like | POST | ✅ | ✅ | ✅ |
| /api/v2/spots/{id}/save | POST | ✅ | ✅ | ✅ |
| /api/v2/routes/{id}/like | POST | ✅ | ✅ | ✅ |
| /api/v2/routes/{id}/save | POST | ✅ | ✅ | ✅ |
| /api/v2/spots/{id}/social | GET | ✅ | ✅ | ✅ |
| /api/v2/routes/{id}/social | GET | ✅ | ✅ | ✅ |

**Verdict**: ✅ 6/6 endpoints exact match

---

### Item 14: FollowService

| Method | Design | Implementation | Status |
|--------|--------|---------------|:------:|
| follow(followerId, followingId) | ✅ | ✅ | ✅ |
| unfollow(followerId, followingId) | ✅ | ✅ | ✅ |
| isFollowing(followerId, followingId) | ✅ | ✅ | ✅ |
| getFollowers(userId, pageable) | ✅ | ✅ | ✅ |
| getFollowing(userId, pageable) | ✅ | ✅ | ✅ |
| Self-follow check (BAD_REQUEST) | ✅ | ✅ | ✅ |
| Duplicate follow check (CONFLICT) | ✅ | ✅ | ✅ |
| Count increment/decrement | ✅ | ✅ | ✅ |
| `@Transactional(readOnly = true)` on reads | -- | ✅ | ✅ (+) |

**Verdict**: ✅ Full match

---

### Item 15: FollowController (5 endpoints)

| Endpoint | Method | Status |
|----------|--------|:------:|
| /api/v2/users/{userId}/follow | POST | ✅ |
| /api/v2/users/{userId}/follow | DELETE | ✅ |
| /api/v2/users/{userId}/follow/status | GET | ✅ |
| /api/v2/users/{userId}/followers | GET | ✅ |
| /api/v2/users/{userId}/following | GET | ✅ |

**Minor difference**: Design injects `UserSyncService` into FollowController but never uses it. Implementation correctly omits this unused dependency.

**Verdict**: ✅ 5/5 endpoints match (unused dep removed = improvement)

---

### Item 16: UserController (4 endpoints)

| Endpoint | Method | Design | Implementation | Status |
|----------|--------|--------|---------------|:------:|
| /api/v2/users/{userId}/profile | GET | ✅ | ✅ | ✅ |
| /api/v2/users/{userId}/likes/spots | GET | ✅ | ✅ | ✅ |
| /api/v2/users/{userId}/saves/routes | GET | ✅ | ✅ | ✅ |
| /api/v2/users/me/saves | GET | ✅ | ✅ | ✅ |

**Minor difference**: Design injects `SocialService` into UserController. Implementation omits it (unused). Correct decision.

**Verdict**: ✅ 4/4 endpoints match

---

### Item 17: UserRouteService

| Method | Design | Implementation | Status |
|--------|--------|---------------|:------:|
| replicate(userId, routeId, scheduledDate) | ✅ | ✅ | ✅ |
| getMyRoutes(userId, status, pageable) | ✅ | ✅ | ✅ |
| updateStatus(userId, myRouteId, status) | ✅ | ✅ | ✅ |
| delete(userId, myRouteId) | ✅ | ✅ | ✅ |
| Ownership check (FORBIDDEN) | ✅ | ✅ | ✅ |
| completedAt auto-set | ✅ | ✅ | ✅ |

**Improvement**: `getMyRoutes` adds `!status.isEmpty()` check in addition to null check. Adds `@Transactional(readOnly = true)` on read method.

**Verdict**: ✅ Full match

---

### Item 18: UserRouteController (5 endpoints)

| Endpoint | Method | Design | Implementation | Status |
|----------|--------|--------|---------------|:------:|
| /api/v2/routes/{routeId}/replicate | POST | ✅ | ✅ | ✅ |
| /api/v2/users/me/routes | GET | ✅ | ✅ | ✅ |
| /api/v2/users/me/routes/{myRouteId} | PATCH | ✅ | ✅ | ✅ |
| /api/v2/users/me/routes/{myRouteId} | DELETE | ✅ | ✅ | ✅ |
| /api/v2/routes/{routeId}/variations | GET | ✅ | ✅ | ✅ |

**Improvement**: DELETE endpoint adds `@ResponseStatus(HttpStatus.NO_CONTENT)` for proper 204 response (design returns void with default 200).

**Verdict**: ✅ 5/5 endpoints match

---

### Item 19: SecurityConfig Update

| Rule | Design | Implementation | Status |
|------|--------|---------------|:------:|
| GET /api/v2/users/** permitAll | ✅ | ✅ | ✅ |
| GET /api/v2/spots/*/social permitAll | ✅ | ✅ (covered by GET /api/v2/spots/**) | ✅ |
| GET /api/v2/routes/*/social permitAll | ✅ | ✅ (covered by GET /api/v2/routes/**) | ✅ |
| PATCH /api/v2/** authenticated | ✅ | ✅ | ✅ |

The implementation uses broader wildcard rules (`GET /api/v2/spots/**` permitAll) that cover the specific social endpoints. This is correct since all GET endpoints should be public.

**Verdict**: ✅ Full match

---

### Item 20: RoutePreviewResponse Check

| Aspect | Status | Notes |
|--------|:------:|-------|
| File exists | ✅ | `dto/response/RoutePreviewResponse.java` |
| `from(Route)` static factory | ✅ | Used by UserRouteController.getVariations() and UserController.getSavedRoutes() |
| Fields: id, slug, title, theme, area, totalDuration, totalDistance, spotCount, likesCount | ✅ | |

**Verdict**: ✅ Confirmed functional

---

## 4. Differences Summary

### Missing Features (Design O, Implementation X)

None.

### Added Features (Design X, Implementation O)

| Item | Implementation Location | Description | Impact |
|------|------------------------|-------------|--------|
| `getCurrentEmail()` | AuthUtil.java:21 | Email extraction from SecurityContext | Low (beneficial) |
| Null-safe email in UserSyncService | UserSyncService.java:21 | Fallback for null email | Low (defensive) |
| `@ResponseStatus(NO_CONTENT)` on delete | UserRouteController.java:62 | Proper HTTP 204 for DELETE | Low (improvement) |
| `@Transactional(readOnly=true)` on reads | SocialService, FollowService, UserRouteService | Optimized read transactions | Low (performance) |
| `@JsonProperty` on boolean DTOs | SocialStatusResponse, FollowStatusResponse | Correct Jackson `is*` serialization | Low (fix) |
| `Math.max(0, ...)` on all decrements | SocialService | Prevents negative count values | Low (defensive) |

### Changed Features (Design != Implementation)

| Item | Design | Implementation | Impact |
|------|--------|----------------|--------|
| SocialStatusResponse field naming | `boolean isLiked` | `boolean liked` + `@JsonProperty("isLiked")` | None (JSON output identical) |
| FollowStatusResponse field naming | `boolean isFollowing` | `boolean following` + `@JsonProperty("isFollowing")` | None (JSON output identical) |
| MyRouteResponse.parentRouteId | Always `route.getId()` | Checks `route.getParentRoute()` first | Low (more correct) |
| FollowController dependencies | Injects UserSyncService | Omits unused UserSyncService | None (cleaner) |
| UserController dependencies | Injects SocialService | Omits unused SocialService | None (cleaner) |

---

## 5. Architecture Compliance

| Layer | Expected | Actual | Status |
|-------|----------|--------|:------:|
| Controller -> Service | ✅ | ✅ | ✅ |
| Controller -> AuthUtil | ✅ | ✅ | ✅ |
| Service -> Repository | ✅ | ✅ | ✅ |
| DTO separate from Entity | ✅ | ✅ | ✅ |
| No circular dependencies | ✅ | ✅ | ✅ |

Architecture Score: **100%**

---

## 6. Convention Compliance

| Category | Convention | Compliance |
|----------|-----------|:----------:|
| Entity naming | PascalCase | 100% |
| Repository naming | {Entity}Repository | 100% |
| Service naming | {Feature}Service | 100% |
| Controller naming | {Feature}Controller | 100% |
| DTO naming | {Purpose}Request/Response | 100% |
| Package structure | domain/entity, domain/repository, service, controller, dto/request, dto/response, security | 100% |
| Lombok usage | @Builder @Getter @Setter | 100% |
| API versioning | /api/v2/* | 100% |
| Korean error messages | ✅ | 100% |

Convention Score: **100%**

---

## 7. Match Rate Calculation

```
Total Items: 20
Fully Matched: 20/20 (all items implemented as designed)
Minor Deviations: 5 (all are improvements or functionally equivalent)
Missing: 0
```

```
+---------------------------------------------+
|  Overall Match Rate: 97%                     |
+---------------------------------------------+
|  Full Match:          20 items (100%)        |
|  Minor Deviations:     5 items (cosmetic)    |
|  Missing:              0 items               |
|  Beneficial Extras:    6 items               |
+---------------------------------------------+
```

Match rate is 97% rather than 100% due to 5 minor deviations from the design spec (field naming strategy, dependency cleanup, parentRouteId logic). All deviations are improvements or functionally equivalent.

---

## 8. Recommended Actions

### Documentation Update (Low Priority)

1. Update design doc SocialStatusResponse/FollowStatusResponse to show `@JsonProperty` approach for `is*` boolean fields
2. Note `getCurrentEmail()` addition in AuthUtil design section
3. Note `@Transactional(readOnly = true)` pattern in service design sections
4. Update MyRouteResponse.parentRouteId to reflect the parentRoute-aware logic

### No Immediate Code Changes Required

All 20 design items are implemented correctly. No gaps require code fixes.

---

## 9. Next Steps

- [ ] Update design document with minor deviations (optional)
- [ ] Write completion report (`social-interactions-backend.report.md`)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-31 | Initial analysis -- 97% match rate, 0 missing items | gap-detector |
