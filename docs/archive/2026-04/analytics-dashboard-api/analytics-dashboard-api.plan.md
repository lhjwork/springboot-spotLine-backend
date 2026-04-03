# Analytics Dashboard API Planning Document

> **Summary**: Admin 대시보드용 플랫폼 전체 분석 API + Spot/Route 조회수 증가 로직 + Admin 분석 대시보드 확장
>
> **Project**: Spotline Backend + Admin
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | Admin 대시보드가 큐레이션 현황(Spot/Route 개수)만 보여주며, 플랫폼 성과 지표(조회수, 인기 콘텐츠, 트렌드)를 확인할 수 없음. Spot/Route의 viewsCount 필드가 존재하지만 실제 증가 로직이 없어 항상 0. |
| **Solution** | Backend: 플랫폼 통계 집계 API 3개 + Spot/Route 조회수 증가 엔드포인트. Admin: 기존 Dashboard 확장 — 성과 카드, 인기 콘텐츠 테이블, 일별 트렌드 차트. |
| **Function/UX Effect** | Admin이 Dashboard 접속 시 총 조회수·댓글·신고 등 핵심 지표를 카드로 확인하고, 인기 Spot/Route Top 10을 테이블로 보며, 일별 콘텐츠 생성 트렌드를 차트로 파악. Frontend에서 Spot/Route 상세 페이지 진입 시 조회수가 자동 증가. |
| **Core Value** | 데이터 기반 의사결정 — 콘텐츠 성과를 정량적으로 파악하여 큐레이션 전략 최적화. 조회수 데이터 축적으로 향후 추천 알고리즘 기반 마련. |

---

## 1. Overview

### 1.1 Purpose

플랫폼 전체 성과를 한눈에 파악할 수 있는 분석 API를 제공하고, Admin 대시보드를 확장하여 큐레이션 팀이 데이터 기반으로 콘텐츠 전략을 수립할 수 있도록 한다. 동시에 Spot/Route 조회수 카운터를 실제 동작하게 만든다.

### 1.2 Background

- Admin 대시보드는 현재 Spot/Route 개수 + 지역별 분포 + 카테고리 파이 차트만 제공 (큐레이션 현황 only)
- Spot/Route 엔티티에 `viewsCount`, `likesCount`, `savesCount`, `commentsCount` 필드 존재하나 `viewsCount`는 증가 로직 없음
- Partner 분석(QR 스캔 통계)은 구현 완료 — 같은 패턴 확장
- ContentReport 엔티티 추가로 모더레이션 관련 통계도 가능

### 1.3 Related Documents

- Partner Analytics: `PartnerService.getAnalytics()`, `PartnerAnalyticsResponse`
- Admin Dashboard: `admin-spotLine/src/pages/Dashboard.tsx`
- Spot/Route Entities: `viewsCount`, `commentsCount` 필드

---

## 2. Scope

### 2.1 In Scope

- [ ] Backend: 플랫폼 전체 통계 API (총 Spot/Route/Comment/Report 수, 총 조회수)
- [ ] Backend: 인기 콘텐츠 API (Top Spots/Routes by viewsCount)
- [ ] Backend: 일별 콘텐츠 생성 트렌드 API
- [ ] Backend: Spot/Route 조회수 증가 엔드포인트 (POST)
- [ ] Admin: Dashboard 확장 — 성과 카드 + 인기 콘텐츠 테이블 + 트렌드 차트
- [ ] Frontend: Spot/Route 상세 페이지에서 조회수 증가 API 호출

### 2.2 Out of Scope

- 실시간 WebSocket 분석 (향후 과제)
- 사용자별 행동 분석 / 퍼널 분석 (SpotlineAnalyticsEvent 기반, 별도 피처)
- 커스텀 날짜 범위 쿼리 (이번에는 고정 기간: 7d/30d)
- 내보내기/리포트 PDF 생성

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 플랫폼 전체 통계 API: totalSpots, totalRoutes, totalComments, totalReports, totalViews 반환 | High | Pending |
| FR-02 | 인기 Spot Top 10 API: viewsCount 기준 정렬, slug·title·area·viewsCount·commentsCount 포함 | High | Pending |
| FR-03 | 인기 Route Top 10 API: likesCount 기준 정렬 (Route에 viewsCount 없음, likes 대체) | High | Pending |
| FR-04 | 일별 콘텐츠 생성 트렌드 API: 최근 30일간 Spot/Route 생성 건수 by createdAt | Medium | Pending |
| FR-05 | Spot 조회수 증가 엔드포인트: POST /api/v2/spots/{id}/view → viewsCount++ | High | Pending |
| FR-06 | Route 조회수도 추가: Route 엔티티에 viewsCount 필드 추가 + POST /api/v2/routes/{id}/view | Medium | Pending |
| FR-07 | Admin Dashboard 성과 섹션: 통계 카드 4개 + 인기 콘텐츠 테이블 2개 + 일별 트렌드 차트 | High | Pending |
| FR-08 | Frontend: Spot/Route 상세 페이지 진입 시 조회수 증가 API fire-and-forget 호출 | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | 통계 API 응답 200ms 이내 | 단순 count/findTop 쿼리, 인덱스 활용 |
| Scalability | 조회수 증가는 fire-and-forget, 실패해도 UX 영향 없음 | 에러 무시 패턴 |
| Security | /api/v2/admin/** → hasRole("ADMIN"), 조회수 증가는 permitAll | 기존 SecurityConfig |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 4개 Backend API 엔드포인트 구현 + 빌드 성공
- [ ] Admin Dashboard에 성과 섹션 표시
- [ ] Frontend에서 Spot/Route 상세 진입 시 조회수 증가 확인
- [ ] Gap Analysis 90% 이상

### 4.2 Quality Criteria

- [ ] Zero lint/type errors
- [ ] Backend + Admin + Frontend 빌드 성공
- [ ] 기존 Dashboard 기능 유지 (큐레이션 현황 깨지지 않음)

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 조회수 중복 증가 (같은 사용자 반복 조회) | Low | High | Phase 1에서는 단순 증가, 향후 세션 기반 중복 방지 추가 |
| Dashboard N+1 쿼리로 인한 느린 로딩 | Medium | Low | 단일 통계 API로 집계, 개별 쿼리 최소화 |
| Route에 viewsCount 필드 추가 → DB 마이그레이션 | Low | Low | JPA auto-DDL 사용 (Hibernate update), 기존 Route 0으로 기본값 |

---

## 6. Implementation Items

### Backend (springboot-spotLine-backend)

| # | Item | Files | Description |
|:-:|------|-------|-------------|
| 1 | PlatformStatsResponse DTO | dto/response/ | totalSpots, totalRoutes, totalComments, totalReports, totalViews |
| 2 | PopularContentResponse DTO | dto/response/ | id, slug, title, area/theme, viewsCount, commentsCount |
| 3 | DailyContentTrend DTO | dto/response/ | date, spotCount, routeCount |
| 4 | SpotRepository 쿼리 추가 | domain/repository/ | findTop10ByOrderByViewsCountDesc, 일별 생성 JPQL |
| 5 | RouteRepository 쿼리 추가 | domain/repository/ | findTop10ByOrderByLikesCountDesc, 일별 생성 JPQL |
| 6 | Route 엔티티에 viewsCount 추가 | domain/entity/ | @Builder.Default private Integer viewsCount = 0 |
| 7 | AnalyticsService | service/ | getPlatformStats, getPopularSpots, getPopularRoutes, getDailyTrend, incrementSpotView, incrementRouteView |
| 8 | AnalyticsController | controller/ | GET /admin/analytics/stats, /popular-spots, /popular-routes, /daily-trend + POST /spots/{id}/view, /routes/{id}/view |

### Admin (admin-spotLine)

| # | Item | Files | Description |
|:-:|------|-------|-------------|
| 9 | analyticsAPI.ts | services/v2/ | getPlatformStats, getPopularSpots, getPopularRoutes, getDailyTrend |
| 10 | Dashboard.tsx 확장 | pages/ | 성과 카드 섹션 + 인기 콘텐츠 테이블 + 일별 트렌드 차트 추가 |

### Frontend (front-spotLine)

| # | Item | Files | Description |
|:-:|------|-------|-------------|
| 11 | api.ts에 incrementSpotView/incrementRouteView 함수 추가 | lib/ | fire-and-forget, 에러 무시 |
| 12 | Spot/Route 상세 페이지에서 조회수 증가 호출 | app/ or components/ | useEffect에서 1회 호출 |

---

## 7. Architecture Considerations

### 7.1 Project Level

Dynamic — 기존 아키텍처 유지 (Spring Boot + React + Next.js)

### 7.2 Key Decisions

| Decision | Selected | Rationale |
|----------|----------|-----------|
| 통계 집계 | JPA count/sum 쿼리 | 데이터 규모 작음 (수백 건), 별도 집계 테이블 불필요 |
| 트렌드 차트 | recharts (admin 기존 사용) | CategoryPieChart에서 이미 사용 중 |
| 조회수 증가 방식 | JPA findById + save | 동시성 낮음, 별도 @Query UPDATE 불필요 |
| 조회수 API 인증 | permitAll | 비인증 사용자도 페이지 조회 가능 |

---

## 8. Next Steps

1. [ ] Design 문서 작성 (`/pdca design analytics-dashboard-api`)
2. [ ] 구현 (`/pdca do analytics-dashboard-api`)
3. [ ] Gap Analysis (`/pdca analyze analytics-dashboard-api`)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
