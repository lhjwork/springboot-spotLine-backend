# Phase 1: 데이터 모델 + Place API 프록시 캐싱 Planning Document

> **Summary**: 기존 Store 모델을 Spot/Route로 진화시키고, 네이버/카카오 Place API 프록시+캐싱 계층을 구축한다.
>
> **Project**: Spotline Backend
> **Version**: 2.0.0
> **Author**: Crew
> **Date**: 2026-03-15
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 현재 Backend는 QR 기반 Store 모델만 지원하며, 경험 기반 Spot/Route 구조와 외부 Place API 연동이 없어 콘텐츠+SEO 전략을 실행할 수 없다. |
| **Solution** | Store → Spot 모델 진화, Route 모델 신설, 네이버/카카오 Place API 프록시+인메모리 캐싱(TTL 24h) 계층을 backend-spotLine에 구축한다. |
| **Function/UX Effect** | Front/Admin이 Spot slug로 요청하면 DB 큐레이션 데이터 + Place API 매장 상세가 병합된 단일 응답을 받아 렌더링만 하면 된다. |
| **Core Value** | 크루 큐레이션(crewNote) + 실시간 매장 정보(Place API)의 병합으로, DB에 매장 상세를 저장하지 않으면서도 풍부한 Spot 페이지를 제공한다. |

| Item | Detail |
|------|--------|
| Feature | Phase 1 — Data Model + Place API Proxy |
| Created | 2026-03-15 |
| Duration | 예상 1~2주 |
| Status | Planning |
| Level | Dynamic |
| Target Repo | backend-spotLine |

---

## 1. Overview

### 1.1 Purpose

Spotline 플랫폼의 경험 기반 소셜 구조(Spot/Route)를 지탱하는 **데이터 계층**을 구축한다. 기존 QR 기반 Store 모델을 Spot으로 진화시키고, Route 모델을 신설하며, 네이버/카카오 Place API를 프록시+캐싱하여 매장 상세 정보를 실시간 제공한다.

### 1.2 Background

- 현재 Backend는 `Store` 모델 (매장 정보 직접 저장) + `Recommendation` (매장 간 추천) 구조
- 전체 Plan에서 확정된 데이터 전략: **DB에는 Spot 기본 정보 + externalPlaceId + crewNote만 저장**, 매장 상세는 외부 Place API 실시간 조회+캐싱
- Phase 2(크루 큐레이션 도구)와 Phase 3(Spot/Route SSR 페이지)의 **선행 조건**
- Cold Start 전략: 런칭 전 200~300 Spot + 15~20 Route를 크루가 사전 등록해야 함

### 1.3 Related Documents

- 전체 Plan: `front-spotLine/docs/01-plan/features/experience-social-platform.plan.md`
- 기존 API 문서: `docs/API_DOCUMENTATION.md`
- 콘텐츠 전환 전략: `front-spotLine/docs/content-based_transition_strategic_proposal.md`

---

## 2. Scope

### 2.1 In Scope

- [ ] **Spot 모델**: 기존 Store 스키마를 기반으로 Spot 모델 신설 (slug, source, crewNote, externalPlace, category 확장)
- [ ] **Route 모델**: RouteSpot 배열, 테마, 통계, 변형 연결을 포함한 Route 모델 신설
- [ ] **Place API 프록시**: 네이버 Place API + 카카오 Place API 검색/상세 조회 프록시 엔드포인트
- [ ] **인메모리 캐싱**: Place API 응답 24h TTL 캐싱 (node-cache 또는 Map 기반)
- [ ] **Spot CRUD API**: 생성, 조회(slug), 목록, 근처 검색, 대량 등록
- [ ] **Route CRUD API**: 생성, 조회(slug), 인기 목록
- [ ] **병합 응답**: `GET /api/v2/spots/:slug`에서 DB Spot + Place API 데이터를 병합하여 반환
- [ ] **기존 API 호환**: 기존 Store/QR/Recommendation API는 유지 (breaking change 없음)
- [ ] **타입 정의**: ISpot, IRoute, PlaceInfo 등 TypeScript 인터페이스

### 2.2 Out of Scope

- Redis 캐싱 (Phase 2+ 에서 트래픽에 따라 전환)
- 소셜 기능 (좋아요, 팔로우, 댓글) — Phase 6
- Route 복제/일정 변환 — Phase 7
- QR 시스템 통합 확장 — Phase 5
- 유저 인증/유저 모델 확장 — Phase 6
- 피드/추천 알고리즘 — Phase 4
- 이미지 업로드 (Spot용) — 기존 S3 시스템 재사용, 별도 작업 불필요

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | Spot 모델: slug, title, category(확장 10종), source(crew/user/qr), crewNote, externalPlace(naverPlaceId, kakaoPlaceId), location(GeoJSON), tags 지원 | High | Pending |
| FR-02 | Route 모델: slug, title, theme(7종), spots(RouteSpot[] 순서 배열), totalDuration, area, creator, stats, parentRoute(변형 추적) | High | Pending |
| FR-03 | `GET /api/v2/places/search?query=&provider=naver|kakao` — Place API 검색 프록시 (크루 큐레이션 도구용) | High | Pending |
| FR-04 | `GET /api/v2/places/:provider/:placeId` — Place API 상세 조회 + 24h 캐싱 | High | Pending |
| FR-05 | `POST /api/v2/spots` — Spot 생성 (crew 큐레이션 / 추후 user) | High | Pending |
| FR-06 | `GET /api/v2/spots/:slug` — Spot 상세 (DB + Place API 병합 응답) | High | Pending |
| FR-07 | `GET /api/v2/spots/nearby?lat=&lng=&radius=` — 근처 Spot 검색 (2dsphere) | Medium | Pending |
| FR-08 | `POST /api/v2/spots/bulk` — Spot 대량 등록 (크루 도구 배치 작업용) | Medium | Pending |
| FR-09 | `POST /api/v2/routes` — Route 생성 | High | Pending |
| FR-10 | `GET /api/v2/routes/:slug` — Route 상세 (포함 Spot들 populate + Place API) | High | Pending |
| FR-11 | `GET /api/v2/routes/popular?area=&theme=&limit=` — 인기 Route 목록 | Medium | Pending |
| FR-12 | Place API 실패 시 graceful degradation: placeInfo: null로 DB 데이터만 반환 | High | Pending |
| FR-13 | 기존 `/api/stores/*`, `/api/recommendations/*` API 유지 (하위 호환) | High | Pending |
| FR-14 | `GET /api/v2/spots/discover?lat=&lng=&radius=` — 위치 기반 Spot 발견 (currentSpot + nextSpot + nearbySpots + popularRoutes 병합 응답) | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | Place API 캐시 히트 시 응답 < 100ms | API 응답시간 로깅 |
| Performance | Place API 캐시 미스 시 응답 < 2s (외부 API 의존) | API 응답시간 로깅 |
| Reliability | Place API 장애 시에도 Spot 기본 정보 정상 반환 | 폴백 테스트 |
| Scalability | 인메모리 캐시 최대 1000건 (300 Spot × 2 provider + 여유) | 메모리 모니터링 |
| API Limit | 네이버 25K/일, 카카오 100K/일 내 운영 | Rate limit 카운터 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] Spot 모델 생성 및 CRUD API 동작
- [ ] Route 모델 생성 및 CRUD API 동작
- [ ] Place API 프록시 엔드포인트 동작 (네이버 + 카카오)
- [ ] `GET /api/v2/spots/:slug`에서 DB + Place API 병합 응답 확인
- [ ] 캐시 히트/미스 동작 확인 (TTL 24h)
- [ ] Place API 장애 시 fallback 동작 확인
- [ ] 기존 Store API 정상 동작 유지
- [ ] TypeScript 타입 체크 통과 (`pnpm type-check`)

### 4.2 Quality Criteria

- [ ] Zero lint errors
- [ ] Build succeeds (`pnpm build`)
- [ ] 기존 데모 시스템 정상 동작 유지

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 네이버/카카오 Place API 키 발급 지연 | High | Medium | 사전에 API 키 신청, Mock 데이터로 개발 병행 |
| Place API 응답 형식 변경 | Medium | Low | 프록시 레이어에서 정규화하여 내부 PlaceInfo 타입으로 변환 |
| 인메모리 캐시 메모리 초과 | Medium | Low | maxKeys 제한 (1000), LRU 전략 적용 |
| Store → Spot 마이그레이션 시 기존 데이터 손실 | High | Low | Spot은 별도 컬렉션으로 신설, 기존 Store 유지, 마이그레이션 스크립트 별도 작성 |
| 네이버/카카오 API rate limit 도달 | Medium | Low | 캐시 TTL 24h로 호출 최소화, 300 Spot 기준 일 최대 600 호출 |

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
| Framework | Express (기존) | Express 4.18 | 기존 backend 유지, 안정성 |
| Database | MongoDB (기존) | MongoDB + Mongoose 8 | 기존 인프라 활용 |
| Cache | node-cache / Map / Redis | node-cache | 인메모리, TTL 자동관리, Phase 1에 적합 |
| Place API Client | axios / fetch | axios | 기존 의존성, 타임아웃/인터셉터 지원 |
| API Versioning | URL prefix | `/api/v2/*` | 기존 `/api/*` 호환 유지하며 신규 엔드포인트 분리 |
| Slug 생성 | slugify / nanoid+title | slugify (한글 → 영문 로마자) | SEO-friendly URL, 한글 제목 지원 |

### 6.3 새로운 폴더 구조 (기존 유지 + 확장)

```
src/
├── models/
│   ├── Store.ts              ← 기존 유지
│   ├── Spot.ts               ← 신규
│   └── Route.ts              ← 신규
├── controllers/
│   ├── storeController.ts    ← 기존 유지
│   ├── spotController.ts     ← 신규
│   └── routeController.ts    ← 신규
├── services/
│   ├── storeService.ts       ← 기존 유지
│   ├── spotService.ts        ← 신규
│   ├── routeService.ts       ← 신규
│   └── placeApiService.ts    ← 신규 (네이버/카카오 프록시+캐싱)
├── routes/
│   ├── stores.ts             ← 기존 유지
│   ├── v2/                   ← 신규 (v2 API 라우트)
│   │   ├── spots.ts
│   │   ├── routes.ts
│   │   └── places.ts
├── types/
│   ├── index.ts              ← 기존 유지
│   └── spot.ts               ← 신규 (ISpot, IRoute, PlaceInfo 등)
└── utils/
    └── placeCache.ts         ← 신규 (인메모리 캐시 유틸)
```

### 6.4 데이터 흐름

```
Front/Admin → GET /api/v2/spots/:slug
                    ↓
            spotController.getBySlug()
                    ↓
            spotService.getSpotDetail(slug)
                    ↓
        ┌── DB: Spot.findOne({ slug }) ──┐
        │   (title, crewNote, tags,      │
        │    naverPlaceId, kakaoPlaceId)  │
        └──────────────┬─────────────────┘
                       ↓
        ┌── placeApiService.getPlaceInfo(placeId) ──┐
        │   캐시 히트 → 즉시 반환                     │
        │   캐시 미스 → 네이버/카카오 API 호출          │
        │            → 캐시 저장 (TTL 24h)            │
        │   API 실패 → placeInfo: null (graceful)     │
        └──────────────┬──────────────────────────────┘
                       ↓
            병합 응답: { spot: {...}, placeInfo: {...} }
```

---

## 7. Convention Prerequisites

### 7.1 Existing Project Conventions

- [x] `CLAUDE.md` has coding conventions section (backend-spotLine/CLAUDE.md)
- [x] ESLint configuration (tsconfig strict mode)
- [x] TypeScript configuration (`tsconfig.json`)
- [x] Code flow: Route → Controller → Service → Model

### 7.2 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **API Versioning** | 없음 (단일 /api/) | `/api/v2/*` prefix 규칙 | High |
| **Slug 규칙** | 없음 | Spot/Route slug 생성 규칙 (영문 소문자+하이픈) | High |
| **Place API 응답 정규화** | 없음 | PlaceInfo 통합 타입 정의 | High |
| **캐시 키 규칙** | 없음 | `place:{provider}:{placeId}` 형식 | Medium |
| **Error handling** | exists (errorHandler.ts) | v2 API 에러 응답 형식 통일 | Medium |

### 7.3 Environment Variables Needed

| Variable | Purpose | Scope | To Be Created |
|----------|---------|-------|:-------------:|
| `NAVER_CLIENT_ID` | 네이버 Place API 인증 | Server | **V** |
| `NAVER_CLIENT_SECRET` | 네이버 Place API 인증 | Server | **V** |
| `KAKAO_REST_API_KEY` | 카카오 Place API 인증 | Server | **V** |
| `MONGODB_URI` | DB 연결 | Server | 기존 |
| `JWT_SECRET` | 인증 | Server | 기존 |

---

## 8. Core Type Definitions (Preview)

```typescript
// ---- Spot ----
type SpotSource = "crew" | "user" | "qr";
type SpotCategory =
  | "cafe" | "restaurant" | "bar"
  | "nature" | "culture" | "exhibition"
  | "walk" | "activity" | "shopping" | "other";

interface ISpot extends Document {
  slug: string;
  title: string;
  description?: string;
  category: SpotCategory;
  source: SpotSource;
  crewNote?: string;                    // 크루 한줄 추천 (차별화 핵심)
  location: {
    address: string;
    coordinates: { type: "Point"; coordinates: [number, number] };
    area: string;                       // "성수", "을지로" 등
  };
  externalPlace: {
    naverPlaceId?: string;
    kakaoPlaceId?: string;
  };
  tags: string[];
  media?: string[];                     // S3 이미지 키
  qrCode?: { qrId: string; isActive: boolean };  // QR 파트너 (향후)
  stats: { likes: number; saves: number; views: number };
  creator: { type: "crew" | "user"; id: string; name: string };
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

// ---- Route ----
type RouteTheme = "date" | "travel" | "walk" | "hangout"
  | "food-tour" | "cafe-tour" | "culture";

interface IRoute extends Document {
  slug: string;
  title: string;
  description?: string;
  theme: RouteTheme;
  area: string;
  spots: RouteSpot[];
  totalDuration: number;                // 분
  totalDistance: number;                 // 미터
  stats: { likes: number; saves: number; replications: number; completions: number };
  creator: { type: "crew" | "user"; id: string; name: string };
  parentRoute?: Types.ObjectId;          // 변형 원본
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

interface RouteSpot {
  spot: Types.ObjectId;                 // Spot ref
  order: number;
  suggestedTime?: string;               // "17:30"
  stayDuration?: number;                // 분
  transitionToNext?: {
    walkingTime: number;
    distance: number;
    note?: string;
  };
}

// ---- Place API 정규화 ----
interface PlaceInfo {
  provider: "naver" | "kakao";
  placeId: string;
  name: string;
  address: string;
  phone?: string;
  category?: string;
  businessHours?: string;
  rating?: number;
  reviewCount?: number;
  photos?: string[];
  url?: string;                         // 네이버/카카오 장소 페이지 URL
  updatedAt: string;                    // 캐시 갱신 시각
}
```

---

## 9. Implementation Order

| Step | Task | Dependencies | Estimated |
|------|------|-------------|-----------|
| 1 | TypeScript 타입 정의 (`types/spot.ts`) | 없음 | 2h |
| 2 | Spot Mongoose 모델 (`models/Spot.ts`) | Step 1 | 3h |
| 3 | Route Mongoose 모델 (`models/Route.ts`) | Step 1 | 3h |
| 4 | Place API 서비스 + 캐싱 (`services/placeApiService.ts`, `utils/placeCache.ts`) | 환경변수 준비 | 4h |
| 5 | Spot CRUD 서비스/컨트롤러/라우트 | Step 2, 4 | 4h |
| 6 | Route CRUD 서비스/컨트롤러/라우트 | Step 3, 5 | 3h |
| 7 | Place API 프록시 라우트 (`routes/v2/places.ts`) | Step 4 | 2h |
| 8 | 병합 응답 로직 (Spot + PlaceInfo) | Step 5, 4 | 2h |
| 9 | 대량 등록 엔드포인트 (`POST /api/v2/spots/bulk`) | Step 5 | 2h |
| 10 | 기존 API 호환 확인 + 통합 테스트 | All | 3h |

---

## 10. Next Steps

1. [ ] Design 문서 작성 (`phase1-data-model-place-api.design.md`)
2. [ ] 네이버/카카오 Place API 키 발급 및 `.env` 설정
3. [ ] 구현 시작 (Step 1~10 순서)
4. [ ] Phase 2(크루 큐레이션 도구)와 연계 확인

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-15 | Initial draft | Crew |
