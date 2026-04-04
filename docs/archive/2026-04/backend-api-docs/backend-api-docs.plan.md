# backend-api-docs Planning Document

> **Summary**: Spring Boot 백엔드에 Swagger/OpenAPI 자동 문서 생성 + 공유 API 문서 갱신
>
> **Project**: springboot-spotLine-backend
> **Version**: 0.0.1-SNAPSHOT
> **Author**: Claude (bkit PDCA)
> **Date**: 2026-04-04
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 70+ API 엔드포인트가 문서화되지 않아 프론트엔드/어드민 개발 시 매번 컨트롤러 코드를 직접 확인해야 한다. 기존 `docs/API_DOCUMENTATION.md`는 레거시 Express API 기준이라 완전히 outdated. |
| **Solution** | springdoc-openapi를 추가하여 Swagger UI + OpenAPI 3.0 JSON 자동 생성. 컨트롤러/DTO에 어노테이션으로 설명 추가. 공유 API 문서를 v2 기준으로 갱신. |
| **Function/UX Effect** | `/swagger-ui.html`에서 모든 API를 인터랙티브하게 탐색/테스트 가능. API 스펙 변경 시 문서가 자동 동기화되어 별도 관리 불필요. |
| **Core Value** | 개발 속도 향상 — front-spotLine, admin-spotLine 개발 시 API 스펙을 즉시 확인하고 테스트할 수 있어 커뮤니케이션 비용 제거. |

---

## 1. Overview

### 1.1 Purpose

Spring Boot 백엔드(16개 컨트롤러, 70+ 엔드포인트, 46개 DTO)에 대한 자동화된 API 문서를 생성하여, 프론트엔드/어드민 개발 시 API 스펙을 즉시 확인할 수 있도록 한다.

### 1.2 Background

- **현재 상태**: Swagger/OpenAPI 미설정. API 스펙 확인 시 컨트롤러 소스코드를 직접 읽어야 함.
- **기존 문서**: `docs/API_DOCUMENTATION.md`는 레거시 Express+MongoDB API(port 3000) 기준으로 완전히 outdated.
- **백엔드 전환 완료**: Express → Spring Boot 3.5 마이그레이션이 완료되어 v2 API가 운영 중.
- **3개 클라이언트 레포**: front-spotLine, admin-spotLine 모두 `/api/v2/*` 엔드포인트 사용 중.

### 1.3 Related Documents

- `springboot-spotLine-backend/CLAUDE.md` — 백엔드 아키텍처 개요
- `docs/API_DOCUMENTATION.md` — 레거시 API 문서 (교체 대상)
- `docs/BUSINESS_PLAN.md` — 사업계획서

---

## 2. Scope

### 2.1 In Scope

- [ ] springdoc-openapi 의존성 추가 및 Swagger UI 설정
- [ ] 16개 컨트롤러에 `@Tag`, `@Operation`, `@ApiResponse` 어노테이션 추가
- [ ] 주요 DTO(Request/Response)에 `@Schema` 어노테이션 추가
- [ ] Security(JWT) 연동 — Swagger UI에서 Bearer token 입력 지원
- [ ] API 그룹핑 (Spot, Route, Place, Media, QR, Social, Admin, Analytics)
- [ ] `docs/API_DOCUMENTATION.md`를 v2 기준으로 교체

### 2.2 Out of Scope

- API 엔드포인트 자체의 변경/추가
- 테스트 코드 작성 (문서 어노테이션만 추가)
- CI/CD 파이프라인 변경
- Postman Collection 생성

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | springdoc-openapi 의존성 추가 (`build.gradle`) | High | Pending |
| FR-02 | OpenAPI 설정 클래스 작성 (제목, 버전, 설명, Security scheme) | High | Pending |
| FR-03 | Swagger UI 접근 경로 설정 (`/swagger-ui.html`) | High | Pending |
| FR-04 | 컨트롤러별 `@Tag` 그룹핑 (8개 그룹) | High | Pending |
| FR-05 | 주요 엔드포인트 `@Operation` + `@ApiResponse` 어노테이션 | High | Pending |
| FR-06 | Request DTO `@Schema` 어노테이션 (필드 설명, 예시값, 필수여부) | Medium | Pending |
| FR-07 | Response DTO `@Schema` 어노테이션 | Medium | Pending |
| FR-08 | JWT Bearer 인증 Swagger UI 지원 (`@SecurityScheme`) | High | Pending |
| FR-09 | `docs/API_DOCUMENTATION.md` v2 기준 갱신 | Medium | Pending |
| FR-10 | dev 프로필에서만 Swagger UI 활성화 (prod 비활성화) | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | Swagger UI 로딩 시 앱 시작 시간 +2초 이내 | bootRun 시간 비교 |
| Security | 운영(prod)에서 Swagger UI 비활성화 | profile별 설정 확인 |
| Maintainability | 어노테이션 기반 자동 문서화 — 코드 변경 시 문서 자동 동기화 | 수동 확인 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] `./gradlew bootRun` 후 `http://localhost:4000/swagger-ui.html` 접근 가능
- [ ] 모든 16개 컨트롤러가 그룹별로 표시됨
- [ ] Swagger UI에서 JWT 토큰 입력 후 인증 필요 API 호출 가능
- [ ] 빌드 성공 (`./gradlew build`)
- [ ] `docs/API_DOCUMENTATION.md`가 v2 엔드포인트 기준으로 갱신

### 4.2 Quality Criteria

- [ ] Zero compile errors
- [ ] 기존 테스트 통과
- [ ] Swagger UI에서 모든 엔드포인트 정상 렌더링

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| springdoc-openapi와 Spring Boot 3.5 호환성 | High | Low | springdoc-openapi v2.8+ 사용 (Spring Boot 3.x 공식 지원) |
| 어노테이션 과다로 컨트롤러 가독성 저하 | Medium | Medium | 간결한 어노테이션만 사용, 별도 설정 클래스로 공통 설정 분리 |
| prod 환경에서 API 문서 노출 | High | Low | `springdoc.swagger-ui.enabled=false` (prod profile) |

---

## 6. Architecture Considerations

### 6.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites | ☐ |
| **Dynamic** | Feature-based modules, BaaS | Web apps with backend | ☒ |
| **Enterprise** | Strict layer separation, DI, microservices | High-traffic systems | ☐ |

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| OpenAPI Library | springdoc-openapi / springfox | springdoc-openapi v2.8+ | Spring Boot 3.x 공식 지원, springfox는 유지보수 중단 |
| 문서 스타일 | 어노테이션 기반 / YAML 수동 작성 | 어노테이션 기반 | 코드와 문서 동기화 자동화 |
| Swagger UI 접근 | 전체 공개 / dev만 | dev 프로필만 | 보안 (prod API 스펙 노출 방지) |

### 6.3 구현 구조

```
src/main/java/com/spotline/api/
├── config/
│   └── OpenApiConfig.java          ← NEW: OpenAPI 설정 (제목, Security, Groups)
├── controller/
│   ├── SpotController.java         ← @Tag, @Operation 추가
│   ├── RouteController.java        ← @Tag, @Operation 추가
│   └── ... (14개 컨트롤러)
├── dto/
│   ├── request/                    ← @Schema 추가
│   └── response/                   ← @Schema 추가
└── application.properties          ← springdoc 설정 추가
```

---

## 7. Implementation Order

| Step | Target | Description |
|------|--------|-------------|
| 1 | `build.gradle` | springdoc-openapi-starter-webmvc-ui 의존성 추가 |
| 2 | `OpenApiConfig.java` | OpenAPI 메타 설정 + SecurityScheme(Bearer JWT) |
| 3 | `application.properties` | springdoc 경로/활성화 설정, prod 비활성화 |
| 4 | 16개 컨트롤러 | `@Tag` + 주요 `@Operation`/`@ApiResponse` 어노테이션 |
| 5 | 주요 Request DTO | `@Schema` 어노테이션 (필수 필드 위주) |
| 6 | 주요 Response DTO | `@Schema` 어노테이션 |
| 7 | `docs/API_DOCUMENTATION.md` | v2 기준 갱신 (Swagger UI 링크 포함) |

---

## 8. Next Steps

1. [ ] Write design document (`backend-api-docs.design.md`)
2. [ ] Implementation
3. [ ] Verify Swagger UI renders all endpoints

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-04 | Initial draft | Claude (bkit PDCA) |
