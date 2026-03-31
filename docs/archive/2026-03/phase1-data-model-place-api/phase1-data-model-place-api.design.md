# Phase 1: 데이터 모델 + Place API 프록시 캐싱 Design Document

> **Summary**: Spring Boot 3.5 + PostgreSQL(Supabase) + Caffeine 캐시 기반으로 Spot/Route 도메인 모델과 네이버/카카오 Place API 프록시 계층을 상세 설계한다.
>
> **Project**: Spotline Backend v2 (springboot-spotLine-backend)
> **Version**: 2.0.0
> **Author**: Crew
> **Date**: 2026-03-15
> **Status**: Draft
> **Planning Doc**: [phase1-data-model-place-api.plan.md](../01-plan/features/phase1-data-model-place-api.plan.md)

---

## 1. Overview

### 1.1 Design Goals

1. **Spot/Route 도메인 모델을 PostgreSQL(Supabase)에 JPA 엔티티로 정의**하여, 크루 큐레이션과 유저 경험 데이터를 저장한다.
2. **네이버/카카오 Place API 프록시 + Caffeine 인메모리 캐싱**(24h TTL)으로 매장 상세 정보를 실시간 제공한다.
3. **DB 데이터 + Place API 데이터를 병합하는 응답 구조**를 설계하여 Front/Admin은 렌더링만 하면 되도록 한다.
4. **기존 backend-spotLine(Express+MongoDB)과 독립적**으로 운영하며, `/api/v2/*`로 새 엔드포인트를 제공한다.

### 1.2 Design Principles

- **DB Minimal**: DB에는 큐레이션 정보(crewNote, tags, externalPlaceId)만 저장, 매장 상세는 외부 API 위임
- **Graceful Degradation**: Place API 장애 시에도 DB 데이터만으로 정상 응답
- **Clean Layering**: Controller → Service → Repository + Infrastructure(PlaceAPI, Cache)
- **Slug-first URL**: 모든 공개 리소스는 UUID가 아닌 slug로 접근 (SEO-friendly)

---

## 2. Architecture

### 2.1 Component Diagram

```
┌─────────────────┐      ┌─────────────────────────────────────┐
│  front-spotLine  │      │  springboot-spotLine-backend         │
│  (Next.js 16)   │─────▶│  (Spring Boot 3.5, port 4000)       │
│                 │      │                                     │
│  admin-spotLine  │─────▶│  ┌──────────┐   ┌───────────────┐  │
│  (큐레이션 도구)  │      │  │Controller│──▶│   Service     │  │
└─────────────────┘      │  └──────────┘   │               │  │
                         │                 │  ┌───────────┐ │  │
                         │                 │  │Repository │ │  │
                         │                 │  │  (JPA)    │ │  │
                         │                 │  └─────┬─────┘ │  │
                         │                 │        │       │  │
                         │                 │  ┌─────▼─────┐ │  │
                         │                 │  │PostgreSQL │ │  │
                         │                 │  │(Supabase) │ │  │
                         │                 │  └───────────┘ │  │
                         │                 │               │  │
                         │                 │  ┌───────────┐ │  │
                         │                 │  │PlaceAPI   │ │  │
                         │                 │  │Service    │─┼──┼──▶ 네이버/카카오 API
                         │                 │  │+Caffeine  │ │  │
                         │                 │  └───────────┘ │  │
                         │                 └───────────────┘  │
                         │                                     │
                         │  ┌───────────┐                     │
                         │  │  AWS S3   │ (이미지 스토리지)     │
                         │  └───────────┘                     │
                         └─────────────────────────────────────┘
```

### 2.2 Data Flow — Spot 상세 조회

```
Client GET /api/v2/spots/{slug}
       │
       ▼
┌─ SpotController.getBySlug(slug) ─┐
│                                   │
│  SpotService.getBySlug(slug)      │
│       │                           │
│       ├── SpotRepository          │
│       │   .findBySlugAndIsActiveTrue(slug)
│       │   → Spot Entity (DB)      │
│       │                           │
│       ├── PlaceApiService         │
│       │   .getPlaceDetail(provider, placeId)
│       │       │                   │
│       │       ├─ Caffeine Cache HIT → 즉시 반환 (<1ms)
│       │       │                   │
│       │       └─ Cache MISS       │
│       │           ├─ 카카오 API 호출 → 정규화 → 캐시 저장 (24h)
│       │           └─ 실패 시 → null (graceful)
│       │                           │
│       └── SpotDetailResponse.from(spot, placeInfo)
│           → 병합 응답 반환         │
└───────────────────────────────────┘
```

### 2.3 Data Flow — Place API 검색 (크루 큐레이션용)

```
Admin GET /api/v2/places/search?query=성수 카페&provider=kakao
       │
       ▼
┌─ PlaceController.search(query, provider, size) ─┐
│                                                   │
│  PlaceApiService.searchKakao(query, size)         │
│       │                                           │
│       └── 카카오 Keyword Search API 호출            │
│           → List<PlaceInfo> 정규화 반환             │
│           (검색은 캐싱하지 않음, 항상 최신 결과)       │
└───────────────────────────────────────────────────┘
```

### 2.4 Dependencies

| Component | Depends On | Purpose |
|-----------|-----------|---------|
| SpotController | SpotService | Spot CRUD API 핸들링 |
| RouteController | RouteService | Route CRUD API 핸들링 |
| PlaceController | PlaceApiService | Place API 검색/상세 프록시 |
| SpotService | SpotRepository, PlaceApiService | Spot 비즈니스 로직 + Place 병합 |
| RouteService | RouteRepository, SpotRepository | Route 비즈니스 로직 |
| PlaceApiService | WebClient, CacheManager | 외부 API 호출 + 캐싱 |

---

## 3. Data Model

### 3.1 Entity Relationship Diagram

```
┌──────────────┐
│    Spot       │
│──────────────│
│ id (UUID PK) │
│ slug (UNIQUE)│        ┌──────────────┐
│ title        │        │  RouteSpot   │        ┌──────────────┐
│ category     │◀───────│──────────────│────────│    Route      │
│ source       │  spot  │ id (UUID PK) │ route  │──────────────│
│ crewNote     │        │ spot_id (FK) │        │ id (UUID PK) │
│ address      │        │ route_id (FK)│        │ slug (UNIQUE)│
│ latitude     │        │ spotOrder    │        │ title        │
│ longitude    │        │ suggestedTime│        │ theme        │
│ area         │        │ stayDuration │        │ area         │
│ naverPlaceId │        │ walkingTime  │        │ totalDuration│
│ kakaoPlaceId │        │ distanceToNxt│        │ totalDistance│
│ tags []      │        │ transitionNot│        │ creator*     │
│ media []     │        └──────────────┘        │ parentRoute  │──┐
│ qrId         │                                │ (self FK)    │  │
│ stats*       │                                │ stats*       │  │
│ creator*     │                                │ variations[] │◀─┘
│ isActive     │                                │ isActive     │
│ timestamps   │                                │ timestamps   │
└──────────────┘                                └──────────────┘

[Spot] 1 ──── N [RouteSpot] N ──── 1 [Route]
[Route] 1 ──── N [Route] (self: parentRoute → variations)
```

### 3.2 Database Schema (PostgreSQL / Supabase)

```sql
-- ============================================
-- Spot: 경험의 단위 (장소 + 순간)
-- ============================================
CREATE TABLE spots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(255) NOT NULL UNIQUE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(20) NOT NULL,    -- CAFE, RESTAURANT, BAR, NATURE, CULTURE, EXHIBITION, WALK, ACTIVITY, SHOPPING, OTHER
    source          VARCHAR(10) NOT NULL,    -- CREW, USER, QR
    crew_note       VARCHAR(500),            -- 크루 한줄 추천 (차별화 핵심)

    -- Location
    address         VARCHAR(500) NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    area            VARCHAR(50) NOT NULL,    -- 성수, 을지로, 연남 등

    -- External Place API IDs
    naver_place_id  VARCHAR(100),
    kakao_place_id  VARCHAR(100),

    -- QR Partner (향후)
    qr_id           VARCHAR(100),
    qr_active       BOOLEAN DEFAULT FALSE,

    -- Stats (denormalized for read performance)
    likes_count     INTEGER DEFAULT 0,
    saves_count     INTEGER DEFAULT 0,
    views_count     INTEGER DEFAULT 0,

    -- Creator
    creator_type    VARCHAR(10) NOT NULL,    -- crew, user
    creator_id      VARCHAR(100),
    creator_name    VARCHAR(100),

    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Spot 보조 테이블 (ElementCollection)
CREATE TABLE spot_tags (
    spot_id UUID NOT NULL REFERENCES spots(id) ON DELETE CASCADE,
    tag     VARCHAR(50) NOT NULL
);

CREATE TABLE spot_media (
    spot_id   UUID NOT NULL REFERENCES spots(id) ON DELETE CASCADE,
    media_key VARCHAR(500) NOT NULL    -- S3 key
);

-- Indexes
CREATE INDEX idx_spot_slug     ON spots(slug);
CREATE INDEX idx_spot_area     ON spots(area);
CREATE INDEX idx_spot_category ON spots(category);
CREATE INDEX idx_spot_source   ON spots(source);
CREATE INDEX idx_spot_active   ON spots(is_active);
CREATE INDEX idx_spot_lat_lng  ON spots(latitude, longitude);
CREATE INDEX idx_spot_tags     ON spot_tags(spot_id);

-- ============================================
-- Route: 경험의 묶음 (여러 Spot의 순서 연결)
-- ============================================
CREATE TABLE routes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                VARCHAR(255) NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    theme               VARCHAR(20) NOT NULL,    -- DATE, TRAVEL, WALK, HANGOUT, FOOD_TOUR, CAFE_TOUR, CULTURE
    area                VARCHAR(50) NOT NULL,

    total_duration      INTEGER DEFAULT 0,       -- 총 소요시간 (분)
    total_distance      INTEGER DEFAULT 0,       -- 총 거리 (미터)

    -- Stats
    likes_count         INTEGER DEFAULT 0,
    saves_count         INTEGER DEFAULT 0,
    replications_count  INTEGER DEFAULT 0,
    completions_count   INTEGER DEFAULT 0,

    -- Creator
    creator_type        VARCHAR(10) NOT NULL,
    creator_id          VARCHAR(100),
    creator_name        VARCHAR(100),

    -- Variation (경험 진화)
    parent_route_id     UUID REFERENCES routes(id),

    is_active           BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_route_slug   ON routes(slug);
CREATE INDEX idx_route_area   ON routes(area);
CREATE INDEX idx_route_theme  ON routes(theme);
CREATE INDEX idx_route_active ON routes(is_active);
CREATE INDEX idx_route_parent ON routes(parent_route_id);

-- ============================================
-- RouteSpot: Route-Spot 연결 (순서, 이동 정보)
-- ============================================
CREATE TABLE route_spots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id            UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    spot_id             UUID NOT NULL REFERENCES spots(id),

    spot_order          INTEGER NOT NULL,        -- 순서 (1부터)
    suggested_time      VARCHAR(10),             -- "17:30"
    stay_duration       INTEGER,                 -- 체류 시간 (분)
    walking_time_to_next INTEGER,                -- 다음까지 도보 (분)
    distance_to_next    INTEGER,                 -- 다음까지 거리 (m)
    transition_note     VARCHAR(200)             -- "골목길로 5분"
);

CREATE INDEX idx_route_spot_route ON route_spots(route_id);
CREATE INDEX idx_route_spot_spot  ON route_spots(spot_id);
```

### 3.3 JPA Entity → Table 매핑 요약

| Entity | Table | Key Annotations |
|--------|-------|-----------------|
| `Spot` | `spots` | `@Entity`, UUID PK, `@ElementCollection`(tags, media) |
| `Route` | `routes` | `@Entity`, UUID PK, self-referencing `@ManyToOne`(parentRoute) |
| `RouteSpot` | `route_spots` | `@Entity`, `@ManyToOne` Route + Spot, `@OrderBy("spotOrder")` |

### 3.4 PlaceInfo (캐시 전용, DB 저장 안 함)

```java
// 네이버/카카오 API 응답을 정규화한 캐시 전용 DTO
PlaceInfo {
    provider: "naver" | "kakao"
    placeId: String
    name: String
    address: String
    phone: String?
    category: String?
    businessHours: String?
    rating: Double?
    reviewCount: Integer?
    photos: List<String>?
    url: String?          // 네이버/카카오 장소 페이지 링크
}
```

---

## 4. API Specification

### 4.1 Endpoint List

| Method | Path | Description | Auth | FR |
|--------|------|-------------|------|-----|
| **GET** | `/api/v2/spots/{slug}` | Spot 상세 (DB + PlaceInfo 병합) | Public | FR-06 |
| **GET** | `/api/v2/spots` | Spot 목록 (area, category 필터, 페이징) | Public | FR-06 |
| **GET** | `/api/v2/spots/nearby` | 근처 Spot 검색 | Public | FR-07 |
| **POST** | `/api/v2/spots` | Spot 생성 | Crew/Admin | FR-05 |
| **POST** | `/api/v2/spots/bulk` | Spot 대량 등록 | Crew/Admin | FR-08 |
| **GET** | `/api/v2/routes/{slug}` | Route 상세 (Spot populate) | Public | FR-10 |
| **GET** | `/api/v2/routes/popular` | 인기 Route 목록 | Public | FR-11 |
| **POST** | `/api/v2/routes` | Route 생성 | Crew/Admin | FR-09 |
| **GET** | `/api/v2/places/search` | Place API 검색 프록시 | Crew/Admin | FR-03 |
| **GET** | `/api/v2/places/{provider}/{placeId}` | Place 상세 (캐싱) | Public | FR-04 |
| **GET** | `/api/v2/spots/discover` | 위치 기반 Spot 발견 (current + next) | Public | FR-14 |
| **GET** | `/health` | 헬스체크 | Public | — |

### 4.2 Detailed Specification

#### `GET /api/v2/spots/{slug}` — Spot 상세 (병합 응답)

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "slug": "seongsu-cafe-onion",
  "title": "성수 카페 어니언",
  "description": "성수동 대표 카페...",
  "category": "CAFE",
  "source": "CREW",
  "crewNote": "빵이 맛있고, 2층 테라스에서 성수 골목이 한눈에 보여요",
  "address": "서울 성동구 성수이로 126",
  "latitude": 37.5447,
  "longitude": 127.0556,
  "area": "성수",
  "naverPlaceId": "1234567890",
  "kakaoPlaceId": "9876543210",
  "tags": ["카페", "브런치", "성수핫플"],
  "media": ["spots/seongsu-cafe-onion/main.jpg"],
  "likesCount": 42,
  "savesCount": 15,
  "viewsCount": 320,
  "creatorType": "crew",
  "creatorName": "Spotline Crew",
  "createdAt": "2026-03-15T10:00:00",
  "placeInfo": {
    "provider": "kakao",
    "placeId": "9876543210",
    "name": "카페 어니언 성수",
    "address": "서울 성동구 성수이로 126",
    "phone": "02-1234-5678",
    "category": "카페",
    "businessHours": "매일 08:00~22:00",
    "rating": 4.5,
    "reviewCount": 1523,
    "photos": ["https://..."],
    "url": "https://place.map.kakao.com/9876543210"
  }
}
```

**placeInfo가 null인 경우** (Place API 장애 또는 placeId 없음):
```json
{
  "id": "...",
  "slug": "hangang-sunset-spot",
  "title": "한강 노을 포인트",
  "category": "NATURE",
  "source": "CREW",
  "crewNote": "일몰 30분 전에 가면 딱 좋아요",
  "placeInfo": null
}
```

#### `GET /api/v2/spots` — Spot 목록

**Query Parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `area` | String | null | 지역 필터 (성수, 을지로 등) |
| `category` | String | null | 카테고리 필터 (CAFE, RESTAURANT 등) |
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 |
| `sort` | String | createdAt,desc | 정렬 |

**Response (200 OK):** Spring Data Page 형식
```json
{
  "content": [ { /* SpotDetailResponse (placeInfo 없음) */ } ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

#### `GET /api/v2/spots/nearby` — 근처 Spot

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `lat` | double | Yes | 위도 |
| `lng` | double | Yes | 경도 |
| `radius` | double | No (default 1.0) | 반경 (km) |

#### `POST /api/v2/spots` — Spot 생성

**Request:**
```json
{
  "title": "성수 카페 어니언",
  "description": "성수동 대표 카페",
  "category": "CAFE",
  "source": "CREW",
  "crewNote": "빵이 맛있고, 2층 테라스가 좋아요",
  "address": "서울 성동구 성수이로 126",
  "latitude": 37.5447,
  "longitude": 127.0556,
  "area": "성수",
  "kakaoPlaceId": "9876543210",
  "tags": ["카페", "브런치"],
  "creatorName": "Spotline Crew"
}
```

**Response (201 Created):** SpotDetailResponse

#### `POST /api/v2/spots/bulk` — 대량 등록

**Request:** `List<CreateSpotRequest>` (최대 50개)

**Response (201 Created):** `List<SpotDetailResponse>`

#### `POST /api/v2/routes` — Route 생성

**Request:**
```json
{
  "title": "성수 주말 데이트 코스",
  "description": "카페 → 갤러리 → 저녁 맛집",
  "theme": "DATE",
  "area": "성수",
  "spots": [
    {
      "spotId": "550e8400-...",
      "order": 1,
      "suggestedTime": "14:00",
      "stayDuration": 60,
      "walkingTimeToNext": 10,
      "distanceToNext": 800,
      "transitionNote": "대림창고 방향으로 직진"
    },
    {
      "spotId": "660e9400-...",
      "order": 2,
      "suggestedTime": "15:10",
      "stayDuration": 45
    }
  ],
  "creatorName": "Spotline Crew"
}
```

#### `GET /api/v2/places/search` — Place API 검색

**Query Parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `query` | String | (required) | 검색어 ("성수 카페") |
| `provider` | String | kakao | naver \| kakao |
| `size` | int | 15 | 결과 수 |

**Response:** `List<PlaceInfo>`

#### `GET /api/v2/spots/discover` — 위치 기반 Spot 발견

유저의 GPS 위치를 기반으로 "현재 Spot" + "다음 추천 Spot"을 반환하는 랜딩 전용 엔드포인트.

**Query Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `lat` | double | No | null | 유저 위도 |
| `lng` | double | No | null | 유저 경도 |
| `radius` | double | No | 1.0 | 탐색 반경 (km) |
| `excludeSpotId` | UUID | No | null | 제외할 Spot ID (새로고침 시) |

**Response (200 OK):**
```json
{
  "currentSpot": {
    "spot": { /* SpotDetailResponse */ },
    "placeInfo": { /* PlaceInfo 또는 null */ },
    "distanceFromUser": 120
  },
  "nextSpot": {
    "spot": { /* SpotDetailResponse */ },
    "placeInfo": { /* PlaceInfo 또는 null */ },
    "distanceFromCurrent": 600,
    "walkingTime": 8
  },
  "nearbySpots": [ /* SpotDetailResponse[] 최대 6개, placeInfo 미포함 */ ],
  "popularRoutes": [ /* RoutePreview[] 최대 3개 */ ],
  "area": "성수",
  "locationGranted": true
}
```

**위치 미제공 시 (lat/lng 없음):**
```json
{
  "currentSpot": { /* 전체 기준 인기 Spot (viewsCount 내림차순) */ },
  "nextSpot": { /* 같은 area 내 다른 인기 Spot */ },
  "nearbySpots": [],
  "popularRoutes": [ /* 전체 인기 Route */ ],
  "area": null,
  "locationGranted": false
}
```

**Next Spot 추천 로직:**
1. currentSpot과 같은 area 내 활성 Spot 조회
2. currentSpot 제외, excludeSpotId 제외
3. 필터: 도보 15분 이내 (약 1.0km)
4. 정렬 우선순위:
   - 카테고리 다양성 (currentSpot과 다른 카테고리 우선)
   - crewNote 있는 Spot 우선
   - viewsCount 내림차순
5. 상위 1개 선택

**도보 시간 계산:**
- `walkingTime = (distanceInMeters / 67)` (분, 평균 보행속도 4km/h)

### 4.3 Error Response Format

```json
{
  "timestamp": "2026-03-15T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Spot을 찾을 수 없습니다: invalid-slug",
  "path": "/api/v2/spots/invalid-slug"
}
```

| Status | 상황 | 메시지 |
|--------|------|--------|
| 400 | Validation 실패 | 필드별 한국어 에러 메시지 |
| 404 | Spot/Route 없음 | "Spot을 찾을 수 없습니다: {slug}" |
| 500 | 서버 오류 | "서버 오류가 발생했습니다" |

---

## 5. Cache Strategy

### 5.1 Caffeine Cache 설정

| 항목 | 값 | 근거 |
|------|-----|------|
| Cache Name | `placeInfo` | Place API 응답 전용 |
| Max Size | 2,000 entries | 300 Spot × 2 provider × 3 (여유) |
| TTL | 24시간 (expireAfterWrite) | 매장 정보 일일 변동 최소 |
| Eviction | LRU (Caffeine 기본) | 오래된 항목 우선 제거 |
| Key Format | `{provider}:{placeId}` | "kakao:9876543210" |

### 5.2 캐시 적용 범위

| API | 캐싱 | 이유 |
|-----|------|------|
| `GET /places/search` | **안 함** | 항상 최신 검색 결과 필요 (크루 도구) |
| `GET /places/{provider}/{placeId}` | **24h 캐시** | Spot 상세 조회 시 매번 호출되므로 |
| `GET /spots/{slug}` 내부 PlaceAPI 호출 | **24h 캐시** (위와 동일) | PlaceApiService에서 통합 처리 |

### 5.3 캐시 무효화

- TTL 만료 시 자동 제거 (24h)
- 수동 무효화: 향후 Admin API에서 `/api/v2/admin/cache/evict?key=kakao:123` 추가 예정
- 서버 재시작 시 캐시 초기화 (인메모리이므로)

---

## 6. Security Considerations

- [x] Input validation: `@Valid` + Bean Validation (`@NotBlank`, `@NotNull`)
- [x] CORS: `CorsConfig`에서 허용 origin 명시적 설정
- [x] Spring Security: STATELESS 세션, CSRF 비활성화 (REST API)
- [x] Place API 키: 환경변수로만 관리, 코드에 하드코딩 없음
- [ ] Supabase JWT 검증: Phase 1에서는 Public 엔드포인트 위주, Phase 6에서 구현
- [ ] Rate Limiting: 향후 `spring-boot-starter-cache` + Bucket4j 적용 검토
- [x] SQL Injection: JPA Prepared Statement 사용으로 방지
- [x] Bulk API: 최대 50개 제한 (서버 부하 방지)

---

## 7. Test Plan

### 7.1 Test Scope

| Type | Target | Tool |
|------|--------|------|
| Unit Test | SpotService, RouteService, PlaceApiService | JUnit 5 + Mockito |
| Integration Test | Repository 쿼리 | @DataJpaTest + H2/TestContainers |
| API Test | Controller 엔드포인트 | @WebMvcTest + MockMvc |
| Cache Test | Caffeine 캐시 히트/미스 | 수동 검증 (로그 확인) |

### 7.2 Test Cases (Key)

- [ ] Spot 생성 → slug 자동 생성 확인
- [ ] Spot 생성 → 중복 slug 시 suffix 추가 확인
- [ ] Spot 상세 조회 → PlaceInfo 병합 응답 확인
- [ ] Spot 상세 조회 → Place API 실패 시 placeInfo: null 확인
- [ ] Route 생성 → Spot 참조 및 순서 확인
- [ ] Route 생성 → totalDuration/totalDistance 자동 계산 확인
- [ ] Place 검색 → 네이버/카카오 프록시 응답 정규화 확인
- [ ] 캐시 히트 → Place API 미호출 확인 (로그)
- [ ] Nearby → 위경도 범위 검색 정상 동작

---

## 8. Clean Architecture

### 8.1 Layer Structure

```
com.spotline.api/
│
├── controller/          ← Presentation Layer
│   ├── SpotController        GET/POST /api/v2/spots
│   ├── RouteController       GET/POST /api/v2/routes
│   ├── PlaceController       GET /api/v2/places
│   └── HealthController      GET /health
│
├── service/             ← Application Layer
│   ├── SpotService           Spot CRUD + PlaceInfo 병합
│   └── RouteService          Route CRUD + Spot 참조
│
├── domain/              ← Domain Layer (순수 도메인)
│   ├── entity/
│   │   ├── Spot              JPA Entity
│   │   ├── Route             JPA Entity
│   │   └── RouteSpot         JPA Entity (관계 테이블)
│   ├── enums/
│   │   ├── SpotCategory      10종
│   │   ├── SpotSource        3종 (CREW, USER, QR)
│   │   └── RouteTheme        7종
│   └── repository/
│       ├── SpotRepository    Spring Data JPA
│       └── RouteRepository   Spring Data JPA
│
├── dto/                 ← Data Transfer Objects
│   ├── request/
│   │   ├── CreateSpotRequest
│   │   └── CreateRouteRequest
│   └── response/
│       └── SpotDetailResponse  (Spot + PlaceInfo 병합)
│
├── infrastructure/      ← Infrastructure Layer
│   ├── place/
│   │   ├── PlaceApiService   네이버/카카오 프록시 + @Cacheable
│   │   └── PlaceInfo         정규화 DTO
│   ├── cache/                (Caffeine 설정은 config/에)
│   └── s3/                   AWS S3 서비스 (향후)
│
└── config/              ← Configuration
    ├── SecurityConfig        Spring Security
    ├── CorsConfig            CORS 설정
    └── CacheConfig           Caffeine Cache 설정
```

### 8.2 Dependency Rules

```
Controller ──→ Service ──→ Repository
                 │
                 └──→ PlaceApiService (Infrastructure)
                         │
                         └──→ WebClient + Caffeine Cache
```

- **Controller**: 요청 파싱, 유효성 검증(`@Valid`), 응답 직렬화만 담당
- **Service**: 비즈니스 로직, 트랜잭션 관리, PlaceInfo 병합
- **Repository**: JPA 쿼리만 담당
- **Infrastructure**: 외부 API 호출, 캐싱, S3 등

---

## 9. Coding Convention

### 9.1 Naming Conventions

| Target | Rule | Example |
|--------|------|---------|
| Entity Class | PascalCase | `Spot`, `Route`, `RouteSpot` |
| Repository | `{Entity}Repository` | `SpotRepository` |
| Service | `{Domain}Service` | `SpotService`, `PlaceApiService` |
| Controller | `{Domain}Controller` | `SpotController` |
| DTO Request | `Create{Entity}Request` | `CreateSpotRequest` |
| DTO Response | `{Entity}DetailResponse` | `SpotDetailResponse` |
| Enum | UPPER_SNAKE_CASE | `CAFE`, `FOOD_TOUR` |
| DB Column | snake_case | `crew_note`, `naver_place_id` |
| API Path | kebab 불필요, 복수형 | `/api/v2/spots`, `/api/v2/routes` |

### 9.2 Import Order (Java)

```java
// 1. java.*
import java.util.List;

// 2. jakarta.*
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

// 3. org.springframework.*
import org.springframework.stereotype.Service;

// 4. 외부 라이브러리
import lombok.*;
import com.github.slugify.Slugify;

// 5. 프로젝트 내부
import com.spotline.api.domain.entity.Spot;
```

### 9.3 Environment Variables

| Variable | Purpose | Scope |
|----------|---------|-------|
| `SUPABASE_DB_URL` | PostgreSQL 연결 URL | Server |
| `SUPABASE_DB_USER` | DB 사용자 | Server |
| `SUPABASE_DB_PASSWORD` | DB 비밀번호 | Server |
| `NAVER_CLIENT_ID` | 네이버 API 인증 | Server |
| `NAVER_CLIENT_SECRET` | 네이버 API 인증 | Server |
| `KAKAO_REST_API_KEY` | 카카오 API 인증 | Server |
| `CORS_ORIGINS` | 허용 CORS origin | Server |

---

## 10. Implementation Order

| Step | Task | Files | Dependencies | Est. |
|------|------|-------|-------------|------|
| 1 | Enum 정의 | `domain/enums/*.java` | 없음 | **완료** |
| 2 | Entity 정의 | `domain/entity/*.java` | Step 1 | **완료** |
| 3 | Repository 정의 | `domain/repository/*.java` | Step 2 | **완료** |
| 4 | DTO 정의 | `dto/request/*.java`, `dto/response/*.java` | Step 2 | **완료** |
| 5 | PlaceInfo + PlaceApiService | `infrastructure/place/*.java` | 환경변수 | **완료** |
| 6 | Cache 설정 | `config/CacheConfig.java` | Step 5 | **완료** |
| 7 | SpotService (병합 로직) | `service/SpotService.java` | Step 3, 5 | **완료** |
| 8 | RouteService | `service/RouteService.java` | Step 3, 7 | **완료** |
| 9 | Controllers | `controller/*.java` | Step 7, 8 | **완료** |
| 10 | Security + CORS | `config/Security*.java`, `config/Cors*.java` | 없음 | **완료** |
| 11 | **GlobalExceptionHandler** | `config/GlobalExceptionHandler.java` | Step 9 | **미구현** |
| 12 | **Route 상세 응답 DTO** | `dto/response/RouteDetailResponse.java` | Step 8 | **미구현** |
| 13 | **Spot 수정/삭제 API** | `SpotController.update/delete` | Step 7 | **미구현** |
| 14 | **Place API 상세 구현** (실제 API 호출 로직) | `PlaceApiService` 완성 | API 키 발급 | **미구현** |
| 15 | **Discover API** (위치 기반 current+next Spot) | `SpotController.discover`, `SpotService.discover`, `dto/response/DiscoverResponse.java` | Step 7, 8 | **미구현** |
| 16 | **통합 테스트** | `src/test/java/...` | All | **미구현** |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-03-15 | Initial draft — Spring Boot 기반 설계 | Crew |
| 0.2 | 2026-03-15 | Discover API 추가 — `GET /api/v2/spots/discover` (위치 기반 currentSpot+nextSpot 병합 응답) | Claude Code |
