# experience-posting Design Document

> **Summary**: 기존 Spot/Route CRUD에 JWT 기반 creator 자동 설정 + 소유권 검증 + 내 콘텐츠 조회 API 추가
>
> **Project**: spotline-backend
> **Date**: 2026-03-31
> **Status**: Draft
> **Planning Doc**: [experience-posting.plan.md](../../01-plan/features/experience-posting.plan.md)

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | Spot/Route CRUD는 존재하지만 creatorType="crew" 하드코딩, creatorId 미설정, 소유권 검증 없음 |
| **Solution** | JWT 기반 creator 자동 설정 + 소유권 검증 + 내 콘텐츠 조회 API 추가 (기존 코드 수정) |
| **Function/UX Effect** | 유저가 직접 Spot/Route를 만들고 관리 — UGC 생태계 시작 |
| **Core Value** | 콘텐츠 공급원을 크루→유저로 확장 — 스케일링의 핵심 전환점 |

---

## 1. Overview

### 1.1 Design Goals

- 기존 CRUD 로직 최소 변경으로 유저 포스팅 지원
- 소유권 검증으로 타인 콘텐츠 수정/삭제 방지
- 레거시 데이터 (creatorId=null) 하위 호환 유지

### 1.2 Design Principles

- 기존 Service 메서드 시그니처에 userId/creatorType 파라미터 추가
- Controller에서 AuthUtil로 JWT userId 추출 → Service에 전달
- 신규 파일 0개 — 기존 파일 수정만으로 완성

---

## 2. Architecture

### 2.1 Data Flow

```
[유저 포스팅]
Client → POST /spots (JWT) → SpotController → authUtil.requireUserId()
  → SpotService.create(request, userId, "user") → Spot(creatorId=userId, creatorType="user")

[소유권 검증]
Client → PUT /spots/{slug} (JWT) → SpotController → authUtil.requireUserId()
  → SpotService.update(slug, request, userId) → spot.creatorId == userId? → 허용/403

[내 콘텐츠]
Client → GET /users/me/spots (JWT) → UserController → authUtil.requireUserId()
  → SpotRepository.findByCreatorIdAndIsActiveTrue(userId) → SimplePageResponse
```

---

## 3. Implementation Details

### 3.1 R-1: SpotRepository — 쿼리 메서드 추가

**파일**: `domain/repository/SpotRepository.java` (수정)

```java
// 추가
Page<Spot> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);
```

### 3.2 R-2: RouteRepository — 쿼리 메서드 추가

**파일**: `domain/repository/RouteRepository.java` (수정)

```java
// 추가
Page<Route> findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(String creatorId, Pageable pageable);
```

### 3.3 M-1: SpotService.create() — creator 파라미터 추가

**파일**: `service/SpotService.java` (수정)

```java
// 기존
@Transactional
public SpotDetailResponse create(CreateSpotRequest request) {
    // ...
    .creatorType("crew")
    .creatorName(request.getCreatorName())
    .build();
}

// 변경
@Transactional
public SpotDetailResponse create(CreateSpotRequest request, String userId, String creatorType) {
    // ...
    .creatorType(creatorType)
    .creatorId(userId)
    .creatorName(request.getCreatorName())
    .build();
}
```

- `bulkCreate()`도 내부에서 `create(request, null, "crew")` 호출하도록 수정

### 3.4 M-2: SpotService.update() — 소유권 검증 추가

**파일**: `service/SpotService.java` (수정)

```java
// 기존
@Transactional
public SpotDetailResponse update(String slug, UpdateSpotRequest request) {
    Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
        .orElseThrow(...);
    // 바로 수정

// 변경
@Transactional
public SpotDetailResponse update(String slug, UpdateSpotRequest request, String userId) {
    Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
        .orElseThrow(...);
    verifyOwnership(spot.getCreatorId(), userId);
    // 기존 수정 로직 동일
```

### 3.5 M-3: SpotService.delete() — 소유권 검증 추가

```java
// 기존
@Transactional
public void delete(String slug) {

// 변경
@Transactional
public void delete(String slug, String userId) {
    Spot spot = spotRepository.findBySlugAndIsActiveTrue(slug)
        .orElseThrow(...);
    verifyOwnership(spot.getCreatorId(), userId);
    spot.setIsActive(false);
    spotRepository.save(spot);
}
```

### 3.6 SpotService — verifyOwnership 헬퍼 메서드

```java
private void verifyOwnership(String creatorId, String userId) {
    // creatorId == null: 레거시 데이터 허용 (하위 호환)
    if (creatorId != null && !creatorId.equals(userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 콘텐츠만 수정/삭제할 수 있습니다");
    }
}
```

### 3.7 M-4: RouteService.create() — creator 파라미터 추가

**파일**: `service/RouteService.java` (수정)

```java
// 기존
@Transactional
public Route create(CreateRouteRequest request) {
    // ...
    .creatorType("crew")
    .creatorName(request.getCreatorName())

// 변경
@Transactional
public Route create(CreateRouteRequest request, String userId, String creatorType) {
    // ...
    .creatorType(creatorType)
    .creatorId(userId)
    .creatorName(request.getCreatorName())
```

- `createAndReturn()` 시그니처도 동일하게 변경:
  `createAndReturn(CreateRouteRequest request, String userId, String creatorType)`

### 3.8 M-5: RouteService.update() — 소유권 검증 추가

```java
// 기존
@Transactional
public RouteDetailResponse update(String slug, UpdateRouteRequest request) {

// 변경
@Transactional
public RouteDetailResponse update(String slug, UpdateRouteRequest request, String userId) {
    Route route = getBySlug(slug);
    verifyOwnership(route.getCreatorId(), userId);
    // 기존 수정 로직 동일
```

### 3.9 M-6: RouteService.delete() — 소유권 검증 추가

```java
// 기존
@Transactional
public void delete(String slug) {

// 변경
@Transactional
public void delete(String slug, String userId) {
    Route route = getBySlug(slug);
    verifyOwnership(route.getCreatorId(), userId);
    route.setIsActive(false);
    routeRepository.save(route);
}
```

### 3.10 RouteService — verifyOwnership 헬퍼 메서드

```java
private void verifyOwnership(String creatorId, String userId) {
    if (creatorId != null && !creatorId.equals(userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 콘텐츠만 수정/삭제할 수 있습니다");
    }
}
```

### 3.11 M-7~M-8: SpotController — AuthUtil 연동

**파일**: `controller/SpotController.java` (수정)

```java
// 필드 추가
private final AuthUtil authUtil;

// create 수정
@PostMapping
public ResponseEntity<SpotDetailResponse> create(@Valid @RequestBody CreateSpotRequest request) {
    String userId = authUtil.requireUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(spotService.create(request, userId, "user"));
}

// update 수정
@PutMapping("/{slug}")
public ResponseEntity<SpotDetailResponse> update(
        @PathVariable String slug,
        @Valid @RequestBody UpdateSpotRequest request) {
    return ResponseEntity.ok(spotService.update(slug, request, authUtil.requireUserId()));
}

// delete 수정
@DeleteMapping("/{slug}")
public ResponseEntity<Void> delete(@PathVariable String slug) {
    spotService.delete(slug, authUtil.requireUserId());
    return ResponseEntity.noContent().build();
}

// bulkCreate는 크루 전용이므로 유지 (creatorType="crew", userId=null)
```

### 3.12 M-9~M-10: RouteController — AuthUtil 연동

**파일**: `controller/RouteController.java` (수정)

```java
// 필드 추가
private final AuthUtil authUtil;

// create 수정
@PostMapping
public ResponseEntity<RouteDetailResponse> create(@Valid @RequestBody CreateRouteRequest request) {
    String userId = authUtil.requireUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(routeService.createAndReturn(request, userId, "user"));
}

// update 수정
@PutMapping("/{slug}")
public ResponseEntity<RouteDetailResponse> update(
        @PathVariable String slug,
        @Valid @RequestBody UpdateRouteRequest request) {
    return ResponseEntity.ok(routeService.update(slug, request, authUtil.requireUserId()));
}

// delete 수정
@DeleteMapping("/{slug}")
public ResponseEntity<Void> delete(@PathVariable String slug) {
    routeService.delete(slug, authUtil.requireUserId());
    return ResponseEntity.noContent().build();
}
```

### 3.13 A-1~A-2: UserController — 내 콘텐츠 조회 엔드포인트

**파일**: `controller/UserController.java` (수정 — 2개 엔드포인트 추가)

```java
// 필드 추가
private final SpotRepository spotRepository;
private final RouteRepository routeRepository;

// A-1: 내 Spot 목록
@GetMapping("/me/spots")
public SimplePageResponse<SpotDetailResponse> getMySpots(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    String userId = authUtil.requireUserId();
    Page<Spot> spots = spotRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
        userId, PageRequest.of(page, size));
    return new SimplePageResponse<>(
        spots.getContent().stream().map(s -> SpotDetailResponse.from(s, null)).toList(),
        spots.hasNext()
    );
}

// A-2: 내 Route 목록
@GetMapping("/me/routes-created")
public SimplePageResponse<RoutePreviewResponse> getMyCreatedRoutes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    String userId = authUtil.requireUserId();
    Page<Route> routes = routeRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc(
        userId, PageRequest.of(page, size));
    return new SimplePageResponse<>(
        routes.getContent().stream().map(RoutePreviewResponse::from).toList(),
        routes.hasNext()
    );
}
```

> **Note**: 기존 `GET /users/me/routes`는 UserRouteController에서 "복제한 Route" 조회용으로 사용 중이므로, 내가 만든 Route는 `/me/routes-created`로 구분

---

## 4. Error Handling

| Code | Endpoint | Cause | Message |
|------|----------|-------|---------|
| 401 | POST/PUT/DELETE | 인증 미제공 | Spring Security 기본 처리 |
| 403 | PUT/DELETE spots, routes | 본인 콘텐츠가 아님 | "본인의 콘텐츠만 수정/삭제할 수 있습니다" |
| 404 | PUT/DELETE | Spot/Route 없음 | 기존 ResourceNotFoundException |

---

## 5. Security Considerations

- [x] `authUtil.requireUserId()`로 인증 강제
- [x] 소유권 검증: creatorId == JWT userId
- [x] 레거시 하위 호환: creatorId==null이면 허용
- [x] 크루 bulkCreate는 기존 로직 유지 (creatorType="crew")

---

## 6. Implementation Order

```
1. [ ] R-1: SpotRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc 추가
2. [ ] R-2: RouteRepository.findByCreatorIdAndIsActiveTrueOrderByCreatedAtDesc 추가
3. [ ] M-1: SpotService.create() 시그니처 변경 (userId, creatorType 파라미터)
4. [ ] M-2: SpotService.update() 소유권 검증 추가
5. [ ] M-3: SpotService.delete() 소유권 검증 추가
6. [ ] M-4: RouteService.create() + createAndReturn() 시그니처 변경
7. [ ] M-5: RouteService.update() 소유권 검증 추가
8. [ ] M-6: RouteService.delete() 소유권 검증 추가
9. [ ] M-7~M-8: SpotController AuthUtil 연동 (create/update/delete)
10. [ ] M-9~M-10: RouteController AuthUtil 연동 (create/update/delete)
11. [ ] A-1: UserController GET /me/spots 추가
12. [ ] A-2: UserController GET /me/routes-created 추가
```

---

## 7. Impact Analysis

| 파일 | 변경 유형 | 변경 내용 |
|------|----------|----------|
| `domain/repository/SpotRepository.java` | 수정 | findByCreatorId 쿼리 추가 |
| `domain/repository/RouteRepository.java` | 수정 | findByCreatorId 쿼리 추가 |
| `service/SpotService.java` | 수정 | create/update/delete 시그니처 변경 + verifyOwnership |
| `service/RouteService.java` | 수정 | create/createAndReturn/update/delete 시그니처 변경 + verifyOwnership |
| `controller/SpotController.java` | 수정 | AuthUtil 연동 (3개 메서드) |
| `controller/RouteController.java` | 수정 | AuthUtil 연동 (3개 메서드) |
| `controller/UserController.java` | 수정 | 내 콘텐츠 조회 2개 엔드포인트 추가 |

총 **수정 7개, 신규 0개**

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.1 | 2026-03-31 | Initial draft |
