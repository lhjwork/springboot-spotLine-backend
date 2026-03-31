# backend-auth Plan

## Executive Summary

| Perspective | Description |
|-------------|-------------|
| **Problem** | 모든 write API가 인증 없이 열려 있고, Admin 로그인이 .env 하드코딩 방식이라 보안 취약 |
| **Solution** | Spring Boot에 Supabase JWT 검증 필터 추가 + Admin을 Supabase Auth로 전환 |
| **Function/UX Effect** | Admin 로그인 시 Supabase 이메일/비밀번호 인증, API 호출 시 JWT 자동 검증 |
| **Core Value** | 프로덕션 배포 전 필수 보안 인프라 확보, 향후 사용자 인증 확장 기반 마련 |

---

## 1. 현황 분석

### 1.1 Spring Boot Backend

| 항목 | 상태 |
|------|------|
| JJWT 라이브러리 (0.12.6) | ✅ build.gradle에 선언됨 |
| `supabase.jwt-secret` property | ✅ application.properties에 선언됨 |
| `/api/v2/admin/**` → `.authenticated()` | ✅ SecurityConfig에 설정됨 |
| JwtFilter (JWT 파싱+검증) | ❌ 미구현 |
| AuthenticationProvider / UserDetailsService | ❌ 미구현 |
| 실제 인증 동작 | ❌ 토큰 검증 불가 (필터 없음) |
| write endpoint 보호 | ❌ `anyRequest().permitAll()` |

### 1.2 Admin (admin-spotLine)

| 항목 | 상태 |
|------|------|
| AuthContext.tsx | ✅ 존재하지만 로컬 .env 비밀번호 비교 방식 |
| axios interceptor (Bearer token) | ✅ 구현됨 — 토큰 자동 첨부 |
| 401 응답 시 로그아웃 | ✅ 구현됨 |
| Supabase JS Client | ❌ 미설치 |
| 실제 인증 | ❌ fake token `local-${Date.now()}` |

### 1.3 Front (front-spotLine) — 범위 외

- Instagram OAuth로 사용자 인증 구현됨 (별도 플로우)
- Backend에 토큰 전달 안 함 (현재 읽기 전용 사용)
- 향후 `backend-social-api` feature에서 연동 예정

---

## 2. 구현 범위

### 2.1 Backend (springboot-spotLine-backend)

| # | 항목 | 설명 |
|---|------|------|
| B-1 | `SupabaseJwtProperties` | `supabase.jwt-secret` 바인딩 @ConfigurationProperties |
| B-2 | `JwtTokenProvider` | JJWT로 Supabase JWT 파싱, 검증, Claims 추출 |
| B-3 | `JwtAuthenticationFilter` | OncePerRequestFilter — Authorization 헤더에서 토큰 추출 → 검증 → SecurityContext 설정 |
| B-4 | `SecurityConfig` 수정 | JwtFilter 등록 + 엔드포인트 권한 매핑 |
| B-5 | `AdminAuthController` | `POST /api/v2/admin/verify` — 토큰 유효성 확인용 (선택) |

### 2.2 Admin (admin-spotLine)

| # | 항목 | 설명 |
|---|------|------|
| A-1 | `@supabase/supabase-js` 설치 | Supabase Auth 클라이언트 |
| A-2 | `supabaseClient.ts` 생성 | Supabase 클라이언트 초기화 |
| A-3 | `AuthContext.tsx` 교체 | 로컬 인증 → Supabase `signInWithPassword` |
| A-4 | `Login.tsx` 수정 | Supabase 에러 메시지 처리 |
| A-5 | `apiClient.ts` 수정 | `admin_token`을 Supabase `access_token`으로 교체 |

### 2.3 범위 외 (Out of Scope)

- front-spotLine 사용자 인증 (Instagram OAuth 유지)
- User 엔티티 / 소셜 그래프 API (별도 feature: `backend-social-api`)
- 역할 기반 접근 제어 (RBAC) — 현재 Admin만 존재
- Supabase Auth 회원가입 (수동으로 Supabase Dashboard에서 Admin 계정 생성)

---

## 3. 구현 순서

```
B-1 SupabaseJwtProperties
  ↓
B-2 JwtTokenProvider (토큰 파싱/검증)
  ↓
B-3 JwtAuthenticationFilter
  ↓
B-4 SecurityConfig 수정 (필터 등록 + 권한)
  ↓
A-1 @supabase/supabase-js 설치
  ↓
A-2 supabaseClient.ts 생성
  ↓
A-3 AuthContext.tsx 교체
  ↓
A-4 Login.tsx 수정
  ↓
A-5 apiClient.ts 수정 (토큰 교체)
```

---

## 4. 핵심 설계 결정

### 4.1 인증 방식: Supabase JWT 검증

- Supabase Auth가 JWT 발급 → Admin에서 `signInWithPassword` 호출
- Spring Boot는 JWT `secret`으로 서명 검증만 수행 (Supabase API 호출 불필요)
- JJWT 라이브러리 사용 (이미 의존성 추가됨)

### 4.2 엔드포인트 권한 전략

| 패턴 | 권한 |
|------|------|
| `GET /api/v2/spots/**`, `GET /api/v2/routes/**`, `GET /api/v2/places/**` | 공개 (permitAll) |
| `POST/PUT/DELETE /api/v2/spots/**`, `POST/PUT/DELETE /api/v2/routes/**` | 인증 필요 (JWT) |
| `POST /api/v2/media/**` | 인증 필요 (JWT) |
| `/health`, `/actuator/**` | 공개 |

### 4.3 Admin 로그인 플로우 (변경 후)

```
Admin → Login.tsx → Supabase signInWithPassword
                      ↓
              Supabase JWT (access_token) 발급
                      ↓
              localStorage에 저장
                      ↓
              apiClient interceptor → Authorization: Bearer {jwt}
                      ↓
              Spring Boot JwtFilter → 검증 → SecurityContext
```

### 4.4 Supabase Admin 계정 관리

- 수동으로 Supabase Dashboard에서 Admin 이메일 계정 생성
- 별도 회원가입 API 미구현 (Admin은 소수 — 수동 관리)

---

## 5. 영향 범위

| 레포 | 파일 | 변경 유형 |
|------|------|----------|
| springboot-backend | `SupabaseJwtProperties.java` | 신규 |
| springboot-backend | `JwtTokenProvider.java` | 신규 |
| springboot-backend | `JwtAuthenticationFilter.java` | 신규 |
| springboot-backend | `SecurityConfig.java` | 수정 |
| springboot-backend | `application.properties` | 수정 (필요 시) |
| admin-spotLine | `package.json` | 수정 (@supabase/supabase-js 추가) |
| admin-spotLine | `supabaseClient.ts` | 신규 |
| admin-spotLine | `AuthContext.tsx` | 수정 (전면 교체) |
| admin-spotLine | `Login.tsx` | 수정 |
| admin-spotLine | `apiClient.ts` | 수정 |

---

## 6. 환경 변수 요구사항

### Backend (.env)
```
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
SUPABASE_JWT_SECRET=your-jwt-secret  # Supabase Dashboard > Settings > API > JWT Secret
```

### Admin (.env.local)
```
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJ...
# VITE_ADMIN_USERNAME, VITE_ADMIN_PASSWORD → 제거
```

---

## 7. 사전 조건 (Prerequisites)

1. Supabase 프로젝트에서 JWT Secret 확인 (Dashboard > Settings > API)
2. Supabase Dashboard에서 Admin 이메일 계정 1개 이상 생성
3. Backend `.env`에 `SUPABASE_JWT_SECRET` 설정
4. Admin `.env.local`에 `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` 설정
