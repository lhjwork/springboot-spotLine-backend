# backend-auth Design

## 1. Backend API 설계 (springboot-spotLine-backend)

### 1.1 SupabaseJwtProperties.java (신규)

```java
// config/SupabaseJwtProperties.java
@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseJwtProperties {
    private String url;
    private String anonKey;
    private String jwtSecret;
}
```

- `application.properties`의 `supabase.*` 바인딩
- `jwtSecret`은 Supabase Dashboard > Settings > API > JWT Secret

### 1.2 JwtTokenProvider.java (신규)

```java
// security/JwtTokenProvider.java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final SupabaseJwtProperties supabaseProps;

    /**
     * JWT 토큰 검증 및 Claims 반환
     * @return Claims if valid, null if invalid
     */
    public Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                supabaseProps.getJwtSecret().getBytes(StandardCharsets.UTF_8)
            );

            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Claims에서 Supabase user ID (sub) 추출
     */
    public String getUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Claims에서 이메일 추출
     */
    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Claims에서 role 추출 (Supabase는 기본 "authenticated")
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
```

- JJWT 0.12.6 API 사용 (`Jwts.parser().verifyWith()`)
- Supabase JWT의 `sub` = user UUID, `email`, `role` = "authenticated"
- HMAC-SHA256 서명 검증

### 1.3 JwtAuthenticationFilter.java (신규)

```java
// security/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            Claims claims = jwtTokenProvider.validateToken(token);

            if (claims != null) {
                String userId = jwtTokenProvider.getUserId(claims);
                String email = jwtTokenProvider.getEmail(claims);
                String role = jwtTokenProvider.getRole(claims);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId,                    // principal
                        null,                      // credentials
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    );

                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: userId={}, email={}", userId, email);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- `OncePerRequestFilter` 확장
- `Authorization: Bearer {token}` 헤더에서 토큰 추출
- 검증 성공 시 `SecurityContext`에 Authentication 설정
- 검증 실패/토큰 없음 시 그냥 통과 (permitAll 엔드포인트용)

### 1.4 SecurityConfig.java (수정)

```java
// config/SecurityConfig.java — 변경 후
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — 읽기 전용
                .requestMatchers(HttpMethod.GET, "/api/v2/spots/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v2/routes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v2/places/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // 인증 필요 — 쓰기 작업
                .requestMatchers(HttpMethod.POST, "/api/v2/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v2/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v2/**").authenticated()
                // 나머지 GET은 허용
                .anyRequest().permitAll()
            )
            // JWT 필터 등록
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**변경 핵심:**
1. `JwtAuthenticationFilter` 주입 + `addFilterBefore` 등록
2. `POST/PUT/DELETE /api/v2/**` → `.authenticated()` (기존 `anyRequest().permitAll()` 제거)
3. 기존 GET permitAll 유지

---

## 2. Admin Frontend 설계 (admin-spotLine)

### 2.1 @supabase/supabase-js 설치

```bash
pnpm add @supabase/supabase-js
```

### 2.2 supabaseClient.ts (신규)

```typescript
// src/lib/supabaseClient.ts
import { createClient } from "@supabase/supabase-js";

const supabaseUrl = import.meta.env["VITE_SUPABASE_URL"];
const supabaseAnonKey = import.meta.env["VITE_SUPABASE_ANON_KEY"];

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error("Supabase 환경 변수가 설정되지 않았습니다");
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
```

### 2.3 AuthContext.tsx (전면 교체)

```typescript
// src/contexts/AuthContext.tsx
import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { supabase } from "../lib/supabaseClient";
import type { Admin } from "../types";
import type { Session } from "@supabase/supabase-js";

interface AuthContextType {
  admin: Admin | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}

function sessionToAdmin(session: Session): Admin {
  const user = session.user;
  return {
    id: user.id,
    username: user.email?.split("@")[0] ?? "admin",
    email: user.email ?? "",
    role: "super_admin",
    isActive: true,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 초기 세션 확인
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session) {
        setAdmin(sessionToAdmin(session));
        setIsAuthenticated(true);
      }
      setLoading(false);
    });

    // 세션 변경 리스너
    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        if (session) {
          setAdmin(sessionToAdmin(session));
          setIsAuthenticated(true);
        } else {
          setAdmin(null);
          setIsAuthenticated(false);
        }
      }
    );

    return () => subscription.unsubscribe();
  }, []);

  const login = async (email: string, password: string) => {
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) {
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const logout = async () => {
    await supabase.auth.signOut();
    setAdmin(null);
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider value={{ admin, isAuthenticated, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
```

**변경 핵심:**
- `signInWithPassword` → Supabase 서버에서 JWT 발급
- `onAuthStateChange` → 토큰 자동 갱신 감지
- `sessionToAdmin()` → Supabase User → Admin 매핑
- localStorage 직접 관리 제거 (Supabase JS가 자동 관리)

### 2.4 Login.tsx (수정)

```typescript
// 변경 사항:
// 1. "사용자명 또는 이메일" → "이메일" (Supabase는 이메일 로그인)
// 2. form field name: username → email
// 3. placeholder 변경
// 4. console.log 제거
// 5. input type="email" 추가

interface LoginFormData {
  email: string;    // username → email
  password: string;
}

// form field 변경:
<label>이메일</label>
<input
  {...register("email", { required: "이메일을 입력하세요" })}
  type="email"
  placeholder="admin@spotline.kr"
/>

// onSubmit 변경:
const result = await login(data.email, data.password);
```

### 2.5 apiClient.ts (수정)

```typescript
// 변경: localStorage 직접 읽기 → Supabase session에서 토큰 획득

import { supabase } from "../../lib/supabaseClient";

// 요청 인터셉터 — Supabase access_token 사용
apiClient.interceptors.request.use(
  async (config) => {
    const { data: { session } } = await supabase.auth.getSession();
    if (session?.access_token) {
      config.headers.Authorization = `Bearer ${session.access_token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 응답 인터셉터 — 401 시 Supabase 로그아웃
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await supabase.auth.signOut();
      window.location.href = "/login";
    }
    const errorMessage = error.response?.data?.message ||
                        error.message ||
                        '서버 오류가 발생했습니다.';
    return Promise.reject({ ...error, message: errorMessage, status: error.response?.status });
  }
);
```

**변경 핵심:**
- `localStorage.getItem("admin_token")` → `supabase.auth.getSession()` 에서 `access_token` 획득
- 401 시 `supabase.auth.signOut()` 호출
- `legacyApiClient`도 동일 패턴 적용 (또는 레거시 제거)

---

## 3. 구현 순서 및 체크리스트

| # | 작업 | 파일 | 레포 |
|---|------|------|------|
| 1 | SupabaseJwtProperties 생성 | `config/SupabaseJwtProperties.java` | backend |
| 2 | JwtTokenProvider 생성 | `security/JwtTokenProvider.java` | backend |
| 3 | JwtAuthenticationFilter 생성 | `security/JwtAuthenticationFilter.java` | backend |
| 4 | SecurityConfig 수정 | `config/SecurityConfig.java` | backend |
| 5 | @supabase/supabase-js 설치 | `package.json` | admin |
| 6 | supabaseClient.ts 생성 | `src/lib/supabaseClient.ts` | admin |
| 7 | AuthContext.tsx 교체 | `src/contexts/AuthContext.tsx` | admin |
| 8 | Login.tsx 수정 | `src/pages/Login.tsx` | admin |
| 9 | apiClient.ts 수정 | `src/services/base/apiClient.ts` | admin |

---

## 4. Supabase JWT 구조 참고

Supabase가 발급하는 JWT payload:

```json
{
  "aud": "authenticated",
  "exp": 1711900800,
  "iat": 1711897200,
  "iss": "https://xxx.supabase.co/auth/v1",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@spotline.kr",
  "role": "authenticated",
  "session_id": "...",
  "app_metadata": { "provider": "email" },
  "user_metadata": {}
}
```

- `sub` → User UUID (Spring Boot에서 principal로 사용)
- `email` → 이메일
- `role` → "authenticated" (Supabase 기본값)
- `exp` → 만료 시간 (기본 1시간, Supabase JS가 자동 갱신)

---

## 5. 환경 변수 체크리스트

### Backend (.env)
```
SUPABASE_JWT_SECRET=your-jwt-secret-from-supabase-dashboard
```
- 기존 `SUPABASE_URL`, `SUPABASE_ANON_KEY`는 이미 설정됨
- `SUPABASE_JWT_SECRET`만 확인 필요

### Admin (.env.local)
```
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJ...
# 아래 두 줄은 제거 가능 (더 이상 사용 안 함)
# VITE_ADMIN_USERNAME=crew
# VITE_ADMIN_PASSWORD=spotline2024
```

---

## 6. 에러 처리 시나리오

| 시나리오 | Backend 동작 | Admin 동작 |
|---------|-------------|-----------|
| 토큰 없음 + GET | 통과 (permitAll) | 정상 |
| 토큰 없음 + POST/PUT/DELETE | 403 Forbidden | 로그인 페이지로 리다이렉트 |
| 토큰 만료 | 401 Unauthorized | Supabase 자동 갱신 시도 → 실패 시 로그아웃 |
| 유효하지 않은 토큰 | 401 Unauthorized | 로그인 페이지로 리다이렉트 |
| Supabase 서버 다운 | JWT 검증은 로컬 (영향 없음) | 로그인 불가 |
