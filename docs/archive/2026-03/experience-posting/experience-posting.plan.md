# experience-posting Plan

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | Spot/Route CRUD API는 존재하지만 creatorType="crew" 하드코딩, creatorId 미설정, JWT 미연동으로 일반 유저가 경험을 포스팅할 수 없음 |
| **Solution** | 기존 생성 로직에 JWT 기반 creator 자동 설정 + 유저 전용 포스팅 엔드포인트 + 내 콘텐츠 조회 API 추가 |
| **Function/UX Effect** | 유저가 직접 Spot/Route를 만들고 관리할 수 있어 UGC(User Generated Content) 생태계 시작 |
| **Core Value** | 플랫폼의 콘텐츠 공급원을 크루에서 유저로 확장 — 콘텐츠 스케일링의 핵심 전환점 |

---

## 1. 현황 분석

### 1.1 기존 구현 — Spot/Route CRUD

| 항목 | 상태 | 문제점 |
|------|:----:|--------|
| POST /api/v2/spots (생성) | ✅ | creatorType="crew" 하드코딩, creatorId 미설정 |
| POST /api/v2/routes (생성) | ✅ | creatorType="crew" 하드코딩, creatorId 미설정 |
| PUT /api/v2/spots/{slug} (수정) | ✅ | 소유권 검증 없음 (누구나 수정 가능) |
| PUT /api/v2/routes/{slug} (수정) | ✅ | 소유권 검증 없음 |
| DELETE /api/v2/spots/{slug} (삭제) | ✅ | 소유권 검증 없음 |
| DELETE /api/v2/routes/{slug} (삭제) | ✅ | 소유권 검증 없음 |
| GET /users/me/spots (내 Spot 목록) | ❌ | 없음 |
| GET /users/me/routes (내 Route 목록) | ❌ | 없음 |
| Media 업로드 인프라 | ✅ | 정상 동작 |

### 1.2 핵심 Gap

1. **Creator 자동 설정 없음**: SpotService.create()에서 `creatorType("crew")` 하드코딩
2. **소유권 검증 없음**: 수정/삭제 시 creatorId vs JWT userId 비교 안 함
3. **내 콘텐츠 조회 없음**: creatorId로 필터링하는 Repository 메서드 없음
4. **유저 포스팅 전용 엔드포인트 없음**: 현재 엔드포인트는 크루용으로 설계됨

---

## 2. 구현 범위

### 2.1 기존 서비스 수정

| # | 파일 | 변경 내용 |
|---|------|----------|
| M-1 | `SpotService.create()` | JWT userId/creatorType 파라미터 추가, creator 자동 설정 |
| M-2 | `SpotService.update()` | 소유권 검증 추가 (creatorId == userId) |
| M-3 | `SpotService.delete()` | 소유권 검증 추가 |
| M-4 | `RouteService.create()` | JWT userId/creatorType 파라미터 추가, creator 자동 설정 |
| M-5 | `RouteService.update()` | 소유권 검증 추가 |
| M-6 | `RouteService.delete()` | 소유권 검증 추가 |

### 2.2 Controller 수정

| # | 파일 | 변경 내용 |
|---|------|----------|
| M-7 | `SpotController.create()` | AuthUtil에서 userId 추출, SpotService.create()에 전달 |
| M-8 | `SpotController.update()/delete()` | AuthUtil에서 userId 추출, 소유권 검증 위임 |
| M-9 | `RouteController.create()` | AuthUtil에서 userId 추출 |
| M-10 | `RouteController.update()/delete()` | AuthUtil에서 userId 추출 |

### 2.3 내 콘텐츠 조회 API (신규)

| # | 엔드포인트 | 설명 |
|---|-----------|------|
| A-1 | `GET /api/v2/users/me/spots` | 내가 만든 Spot 목록 (페이지네이션) |
| A-2 | `GET /api/v2/users/me/routes` | 내가 만든 Route 목록 (페이지네이션) |

### 2.4 Repository 추가

| # | 파일 | 변경 내용 |
|---|------|----------|
| R-1 | `SpotRepository` | `findByCreatorIdAndIsActiveTrue()` 추가 |
| R-2 | `RouteRepository` | `findByCreatorIdAndIsActiveTrue()` 추가 |

### 2.5 범위 외 (Out of Scope)

- Front 포스팅 UI (별도 feature)
- 크루 전용 bulk API 수정 (기존 유지)
- 포스팅 승인/검수 프로세스 (MVP 불필요)
- 태그 자동추천, AI 요약 (추후 feature)

---

## 3. 핵심 설계 결정

### 3.1 Creator 설정 전략

```
기존: SpotService.create(request) → creatorType="crew" 하드코딩
변경: SpotService.create(request, userId, creatorType) → 파라미터로 받음

Controller에서:
- SpotController: authUtil.requireUserId() → "user" 타입으로 생성
- 크루 admin은 별도 admin API 또는 creatorType 지정 가능
```

### 3.2 소유권 검증 전략

```
수정/삭제 시:
1. Spot/Route의 creatorId를 확인
2. creatorId == JWT userId이면 허용
3. creatorId != userId이면 403 Forbidden
4. creatorId == null (레거시 데이터)이면 허용 (하위 호환)
```

### 3.3 크루 vs 유저 구분

- `source` 필드로 구분: `CREW` (크루 큐레이션), `USER` (유저 포스팅)
- 크루 Spot은 `crewNote` 작성 가능, 유저 Spot은 `crewNote` null
- 동일한 CRUD API 공유, creator 정보로 구분

### 3.4 내 콘텐츠 조회

```
GET /api/v2/users/me/spots?page=0&size=20
GET /api/v2/users/me/routes?page=0&size=20
→ creatorId == JWT userId인 항목만 필터링
→ isActive=true만 반환
→ 최신순 정렬
```

---

## 4. 구현 순서

```
R-1 SpotRepository.findByCreatorIdAndIsActiveTrue 추가
  ↓
R-2 RouteRepository.findByCreatorIdAndIsActiveTrue 추가
  ↓
M-1~M-3 SpotService 수정 (creator 파라미터 + 소유권 검증)
  ↓
M-4~M-6 RouteService 수정 (creator 파라미터 + 소유권 검증)
  ↓
M-7~M-8 SpotController 수정 (AuthUtil 연동)
  ↓
M-9~M-10 RouteController 수정 (AuthUtil 연동)
  ↓
A-1~A-2 UserController에 내 콘텐츠 조회 엔드포인트 추가
```

---

## 5. 영향 범위

| 파일 | 변경 유형 |
|------|----------|
| `domain/repository/SpotRepository.java` | 수정 (쿼리 메서드 추가) |
| `domain/repository/RouteRepository.java` | 수정 (쿼리 메서드 추가) |
| `service/SpotService.java` | 수정 (create/update/delete 시그니처 변경) |
| `service/RouteService.java` | 수정 (create/update/delete 시그니처 변경) |
| `controller/SpotController.java` | 수정 (AuthUtil 연동) |
| `controller/RouteController.java` | 수정 (AuthUtil 연동) |
| `controller/UserController.java` | 수정 (내 콘텐츠 조회 2개 추가) |

총 **수정 7개, 신규 0개** — 기존 코드 개선 feature

---

## 6. Front API 스펙

### GET /users/me/spots — Response

```json
{
  "content": [
    {
      "id": "uuid",
      "slug": "cafe-name",
      "title": "카페 이름",
      "category": "CAFE",
      "area": "성수",
      "likesCount": 5,
      "savesCount": 3,
      "createdAt": "2026-03-31T..."
    }
  ],
  "hasNext": true
}
```

### GET /users/me/routes — Response

```json
{
  "content": [
    {
      "id": "uuid",
      "slug": "date-course",
      "title": "데이트 코스",
      "theme": "DATE",
      "area": "성수",
      "spotCount": 4,
      "likesCount": 10,
      "replicationsCount": 3,
      "createdAt": "2026-03-31T..."
    }
  ],
  "hasNext": true
}
```

---

## 7. 사전 조건

1. ✅ Spot/Route CRUD API 존재
2. ✅ AuthUtil (JWT → userId) 존재
3. ✅ UserSyncService (lazy user sync) 존재
4. ✅ SpotSource enum에 USER 값 존재
5. ✅ Spot/Route 엔티티에 creatorId, creatorType 필드 존재
