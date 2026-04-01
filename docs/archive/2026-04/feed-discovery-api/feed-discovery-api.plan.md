# Feed Discovery API Planning Document

> **Summary**: 프론트엔드 피드/디스커버리 페이지가 필요로 하는 누락 API 엔드포인트를 구현하고, 기존 피드 API의 정렬/필터링을 강화하여 콘텐츠 소비 경험을 완성한다.
>
> **Project**: Spotline (springboot-spotLine-backend)
> **Version**: 1.0.0
> **Author**: Crew
> **Date**: 2026-04-01
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 프론트엔드가 호출하는 `GET /api/v2/spots/{id}/routes` 엔드포인트가 백엔드에 존재하지 않아 Spot 상세 페이지의 "이 Spot이 포함된 Route" 섹션이 동작하지 않으며, 피드 Spot 목록에 기본 정렬이 없고, Route 카드에 커버 이미지가 없어 시각적 매력이 부족하다. |
| **Solution** | 3개 누락 API를 추가하고(`spots/{id}/routes`, 피드 정렬 파라미터, Route 커버 이미지), 기존 Spot list API에 기본 정렬(`viewsCount DESC`)을 적용하여 프론트-백엔드 API 완결성을 달성한다. |
| **Function/UX Effect** | Spot 상세 → 관련 Route 탐색이 가능해지고, 피드에서 인기순/최신순 정렬로 콘텐츠 발견이 용이해지며, Route 카드에 커버 이미지가 표시되어 클릭률이 높아진다. |
| **Core Value** | 이미 구현된 프론트엔드 UI가 100% 동작하도록 백엔드 API 갭을 해소하여, 서비스 런칭 전 피드/디스커버리 시스템의 완결성을 확보한다. |

| Item | Detail |
|------|--------|
| Feature | Feed Discovery API (Backend Gap 해소) |
| Created | 2026-04-01 |
| Duration | 예상 2~3일 |
| Status | Planning |
| Level | Dynamic |
| Target Repo | springboot-spotLine-backend |

---

## 1. Overview

### 1.1 Purpose

프론트엔드(front-spotLine)의 피드, 디스커버리, Spot 상세 페이지가 호출하는 API 중 백엔드에 누락된 엔드포인트를 구현하고, 기존 API의 정렬/응답 데이터를 보강하여 프론트-백엔드 API 완결성을 달성한다.

### 1.2 Background

- **Phase 4 (Experience Feed)** 프론트엔드는 이미 완성 (95% Match Rate, 2026-03-28)
- **Spot 상세 페이지**도 완성 (100% Match Rate), `fetchSpotRoutes(spotId)` 호출이 존재하나 백엔드 미구현
- 피드 페이지에서 `GET /api/v2/spots`를 호출하지만 기본 정렬이 없어 DB 삽입 순서로 표시
- `RoutePreviewResponse`에 커버 이미지 필드가 없어 Route 카드가 텍스트로만 구성
- 프론트엔드는 피드 로드 시 Spot/Route 2개 API를 별도 호출 → 네트워크 최적화 여지

### 1.3 Related Documents

- Backend Phase 1 Plan: `springboot-spotLine-backend/docs/01-plan/features/phase1-data-model-place-api.plan.md`
- Frontend Plan: `front-spotLine/docs/01-plan/features/experience-social-platform.plan.md`
- Location-Based Discovery Plan: `front-spotLine/docs/01-plan/features/location-based-discovery.plan.md`

---

## 2. Scope

### 2.1 In Scope

- [ ] **FR-01**: `GET /api/v2/spots/{spotId}/routes` — 특정 Spot이 포함된 Route 목록 반환
- [ ] **FR-02**: Spot list API(`GET /api/v2/spots`)에 `sort` 파라미터 추가 (`popular`, `newest`, `nearest`)
- [ ] **FR-03**: Spot list API 기본 정렬을 `viewsCount DESC`로 설정
- [ ] **FR-04**: `RoutePreviewResponse`에 `coverImageUrl` 필드 추가 (첫 번째 Spot의 첫 미디어)
- [ ] **FR-05**: Route popular API(`GET /api/v2/routes/popular`)에 `sort` 파라미터 추가 (`popular`, `newest`)
- [ ] **FR-06**: 프론트엔드 `fetchSpotRoutes` 응답 타입과 일치하는 DTO 구조 확인

### 2.2 Out of Scope

- 통합 Feed API (`GET /api/v2/feed`) — 향후 최적화로 별도 진행
- Trending/시간 가중치 정렬 — 데이터 축적 후 Phase 4+ 에서 진행
- Route entity에 `coverImageUrl` 컬럼 추가 — 런타임 파생으로 처리
- 프론트엔드 코드 변경 — 백엔드 API만 구현 (프론트는 이미 호출 코드 존재)
- 검색(Search) API — 별도 피처

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | `GET /api/v2/spots/{spotId}/routes`: spotId로 해당 Spot이 포함된 활성 Route 목록 반환. `RoutePreview[]` 형태, `route_spots` 조인 테이블 활용, `likesCount DESC` 정렬, 최대 10개 | High | Pending |
| FR-02 | `GET /api/v2/spots`에 `sort` 쿼리 파라미터 지원: `popular`(viewsCount DESC), `newest`(createdAt DESC). 기본값 `popular` | High | Pending |
| FR-03 | `RoutePreviewResponse`에 `coverImageUrl` 필드 추가. Route의 첫 번째 Spot(orderIndex=0)의 첫 미디어 URL을 사용. 미디어가 없으면 null | Medium | Pending |
| FR-04 | `GET /api/v2/routes/popular`에 `sort` 쿼리 파라미터 지원: `popular`(likesCount DESC, 기존 동작), `newest`(createdAt DESC) | Medium | Pending |
| FR-05 | `GET /api/v2/spots/{spotId}/routes` 응답이 프론트엔드 `RoutePreview` 타입과 일치 (id, slug, title, theme, area, totalDuration, totalDistance, spotCount, likesCount, coverImageUrl) | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | `spots/{spotId}/routes` 응답 < 200ms (Spot당 Route 평균 2~3개) | Backend 로깅 |
| Performance | Spot list 정렬 변경으로 기존 대비 응답 시간 차이 < 50ms | Backend 로깅 |
| Compatibility | 기존 프론트엔드 API 호출 코드 변경 없이 동작 | 수동 검증 |
| Data Integrity | coverImageUrl은 S3 presigned URL이 아닌 CDN/공개 URL 사용 | 코드 리뷰 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] `GET /api/v2/spots/{spotId}/routes` 호출 시 해당 Spot이 포함된 Route 프리뷰 목록 반환
- [ ] `GET /api/v2/spots?sort=popular` 호출 시 viewsCount 내림차순 정렬
- [ ] `GET /api/v2/spots?sort=newest` 호출 시 createdAt 내림차순 정렬
- [ ] `GET /api/v2/routes/popular?sort=newest` 호출 시 createdAt 내림차순 정렬
- [ ] Route 카드 응답에 `coverImageUrl` 필드 포함 (Spot 미디어 기반)
- [ ] 프론트엔드 Spot 상세 페이지에서 "이 Spot이 포함된 Route" 섹션 정상 렌더링
- [ ] 기존 API 동작에 영향 없음 (하위 호환)

### 4.2 Quality Criteria

- [ ] `./gradlew test` 전체 통과
- [ ] `./gradlew build` 성공
- [ ] 새 엔드포인트에 대한 통합 테스트 작성 (최소 3개)

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| route_spots 조인 쿼리 성능 (Spot에 연결된 Route 다수) | Medium | Low | 최대 10개 제한 + 인덱스 확인 |
| coverImageUrl을 위해 Route→Spot→Media 3단계 조인 필요 | Medium | Medium | Fetch Join 또는 서브쿼리로 N+1 방지 |
| sort 파라미터 추가 시 기존 프론트엔드 동작 변경 | Low | Low | 기본값 `popular`로 설정하여 기존 동작 유지 |
| Spot에 미디어가 없는 경우 coverImageUrl = null | Low | High (초기) | null 허용, 프론트에서 플레이스홀더 처리 (이미 구현됨) |

---

## 6. Architecture Considerations

### 6.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites | |
| **Dynamic** | Feature-based modules, BaaS integration | Web apps with backend | **V** |
| **Enterprise** | Strict layer separation, DI, microservices | High-traffic systems | |

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| Spot→Route 조회 위치 | SpotController / RouteController | SpotController | 프론트엔드가 `/spots/{id}/routes`로 호출, URL 구조 일관성 |
| coverImageUrl 계산 | DB 컬럼 / 런타임 파생 | 런타임 파생 | Route 엔티티 수정 불필요, Spot 미디어 변경 시 자동 반영 |
| sort 파라미터 구현 | Spring Pageable / 커스텀 Enum | 커스텀 Enum (`FeedSort`) | 허용된 값만 받아 SQL injection 방지, 비즈니스 의미 명확 |
| N+1 방지 | Fetch Join / EntityGraph / 별도 쿼리 | JPQL Fetch Join | Route→Spots→Media를 한 번에 조회 |

### 6.3 기존 시스템과의 관계

```
프론트엔드 (이미 구현됨)
├── /spot/[slug]/page.tsx → fetchSpotRoutes(spotId) ← FR-01 (신규)
├── /feed/page.tsx → fetchFeedSpots(area, category) ← FR-02 (정렬 강화)
├── /city/[name]/page.tsx → fetchFeedSpots(area) ← FR-02 (정렬 강화)
└── 공통 → RoutePreview 타입 → coverImageUrl ← FR-03 (신규 필드)

백엔드 변경 범위
├── SpotController → spots/{spotId}/routes 엔드포인트 추가
├── SpotService → findRoutesBySpotId() 메서드 추가
├── RouteRepository → 조인 쿼리 추가
├── RoutePreviewResponse → coverImageUrl 필드 추가
├── SpotService.list() → sort 파라미터 처리
├── SpotRepository → 정렬별 쿼리 메서드 추가
└── RouteService → sort 파라미터 처리
```

---

## 7. Convention Prerequisites

### 7.1 Existing Project Conventions

- [x] Controller → Service → Repository 계층 구조
- [x] DTO Request/Response 분리
- [x] `@RequiredArgsConstructor` + `final` 필드 DI
- [x] `@Transactional(readOnly = true)` 기본 설정
- [x] Lombok `@Builder`, `@Data` 패턴

### 7.2 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **FeedSort Enum** | 없음 | `popular`, `newest` 값 정의 | High |
| **sort 파라미터 기본값** | 없음 | `popular` (viewsCount DESC) | High |
| **coverImageUrl 생성** | 없음 | Route.spots(orderIndex=0).media(0).url + S3 base URL | Medium |

---

## 8. Core Type Definitions (Preview)

```java
// ---- Sort Enum ----
public enum FeedSort {
    POPULAR,    // viewsCount DESC (Spot) / likesCount DESC (Route)
    NEWEST      // createdAt DESC
}

// ---- RoutePreviewResponse 확장 ----
@Data @Builder
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
    private String coverImageUrl;  // NEW: 첫 번째 Spot의 첫 미디어 URL

    public static RoutePreviewResponse from(Route route) { ... }
    public static RoutePreviewResponse from(Route route, String s3BaseUrl) { ... } // NEW
}

// ---- RouteRepository 추가 쿼리 ----
@Query("SELECT DISTINCT r FROM Route r JOIN r.spots rs WHERE rs.spot.id = :spotId AND r.isActive = true ORDER BY r.likesCount DESC")
List<Route> findBySpotId(@Param("spotId") UUID spotId);
```

```typescript
// ---- 프론트엔드 기존 RoutePreview 타입 (변경 없음) ----
interface RoutePreview {
  id: string;
  slug: string;
  title: string;
  theme: RouteTheme;
  area: string;
  totalDuration: number;
  totalDistance: number;
  spotCount: number;
  likesCount: number;
  coverImageUrl?: string;  // 기존에 정의되어 있으나 백엔드 미전송
}
```

---

## 9. Implementation Order

| Step | Task | Dependencies | Estimated |
|------|------|-------------|-----------|
| 1 | `FeedSort` Enum 생성 (`domain/enums/`) | 없음 | 15m |
| 2 | `RouteRepository`에 `findBySpotId()` JPQL 쿼리 추가 | 없음 | 30m |
| 3 | `RoutePreviewResponse`에 `coverImageUrl` 필드 + `from(Route, String)` 오버로드 추가 | 없음 | 30m |
| 4 | `SpotService.findRoutesBySpotId()` 메서드 구현 | Step 2, 3 | 1h |
| 5 | `SpotController`에 `GET /{spotId}/routes` 엔드포인트 추가 | Step 4 | 30m |
| 6 | `SpotRepository`에 정렬별 쿼리 메서드 추가 (popular, newest) | Step 1 | 30m |
| 7 | `SpotService.list()`에 `FeedSort` 파라미터 처리 추가 | Step 1, 6 | 1h |
| 8 | `SpotController.list()`에 `sort` 쿼리 파라미터 추가 | Step 7 | 15m |
| 9 | `RouteRepository`에 newest 정렬 쿼리 추가 | Step 1 | 30m |
| 10 | `RouteService.getPopularPreviews()`에 sort 파라미터 처리 추가 | Step 9 | 30m |
| 11 | `RouteController.popular()`에 `sort` 쿼리 파라미터 추가 | Step 10 | 15m |
| 12 | 통합 테스트 작성 (SpotController, RouteController) | All | 2h |
| 13 | 프론트엔드 연동 검증 (Spot 상세 페이지 Route 섹션) | Step 5 | 30m |

**총 예상 시간**: ~8h (2일)

---

## 10. Next Steps

1. [ ] Design 문서 작성 (`/pdca design feed-discovery-api`)
2. [ ] RouteRepository JPQL 쿼리 설계 (N+1 방지 전략)
3. [ ] coverImageUrl 생성 로직 상세 설계 (S3 base URL 처리)
4. [ ] 통합 테스트 시나리오 정의

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-04-01 | Initial draft — Feed Discovery API 백엔드 갭 해소 Plan | Claude Code |
