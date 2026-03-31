# Spotline Backend v2 — Spring Boot

## Tech Stack
- **Runtime**: Java 21 (OpenJDK)
- **Framework**: Spring Boot 3.5.0
- **Database**: PostgreSQL (Supabase hosted)
- **Auth**: Supabase Auth (JWT verification)
- **Cache**: Caffeine (in-memory, TTL 24h)
- **Storage**: AWS S3 (기존 유지)
- **Build**: Gradle (Groovy DSL) + Gradle Wrapper
- **Place API**: 네이버/카카오 Place API 프록시

## Commands
- `./gradlew bootRun` — 개발 서버 실행 (port 4000)
- `./gradlew build` — 빌드
- `./gradlew test` — 테스트
- `./gradlew clean` — 빌드 캐시 정리

## Architecture

```
src/main/java/com/spotline/api/
├── config/              — Spring 설정 (Security, CORS, Cache)
├── controller/          — REST API 컨트롤러
├── service/             — 비즈니스 로직
├── domain/
│   ├── entity/          — JPA 엔티티 (Spot, Route, RouteSpot)
│   ├── enums/           — Enum 타입 (SpotCategory, RouteTheme 등)
│   └── repository/      — Spring Data JPA Repository
├── dto/
│   ├── request/         — API 요청 DTO
│   └── response/        — API 응답 DTO
└── infrastructure/
    ├── place/           — 네이버/카카오 Place API 프록시 + 캐싱
    ├── cache/           — 캐시 유틸
    └── s3/              — AWS S3 서비스
```

## Code Flow
`Controller → Service → Repository (+ PlaceApiService)` 순서로 흐른다.

## API Versioning
- `/api/v2/*` — 신규 Spot/Route/Place API
- 기존 Express `/api/*`는 backend-spotLine에서 유지 (점진적 마이그레이션)

## Key Conventions
- Entity: `@Builder` + `@Getter/@Setter` (Lombok)
- DTO: Request/Response 분리
- Place API 캐싱: Caffeine, 키 형식 `{provider}:{placeId}`, TTL 24h
- Slug: `slugify` 라이브러리, 중복 시 `-{n}` suffix
- 한글 UI 텍스트/에러 메시지, 영어 코드
- 환경변수: `application.properties`에서 `${ENV_VAR:default}` 패턴

## Environment
`.env.example` 참고. 필수:
- `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `KAKAO_REST_API_KEY`
