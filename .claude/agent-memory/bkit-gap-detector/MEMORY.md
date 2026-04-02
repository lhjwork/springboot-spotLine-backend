# Gap Detector Memory - Spotline Backend

## Project: springboot-spotLine-backend
- Spring Boot 3.5 + PostgreSQL (Supabase) + Caffeine Cache
- Package root: `com.spotline.api`
- Design docs: `docs/02-design/features/`
- Analysis output: `docs/03-analysis/`

## Phase 1 Analysis History
- Feature: phase1-data-model-place-api
- v0.1 (2026-03-19): Match Rate **85%** -- 3 missing: bulk limit, idx_route_parent, discover radius
- v0.2 (2026-03-19): Match Rate **96%** (+11%) -- All 3 gaps resolved, 13 new tests (26 total)
- 12/12 design endpoints, 13/13 indexes, 8/9 design test cases covered
- Remaining: 1 cache-hit integration test, design doc Section 10 sync needed
- 11 beneficial extras: SpotMedia, Media API, address decomposition, external links, S3

## SEO Structured Data Analysis History
- Feature: seo-structured-data (cross-repo: front-spotLine + backend)
- v0.1 (2026-03-31): Match Rate **97%** -- 0 missing, 5 minor deviations
- 5/5 new files, 6/6 modified files, 2/2 backend endpoints
- Minor deviations: SlugResponse class vs record, repo query strategy, method naming
- Design doc at: front-spotLine/docs/02-design/features/seo-structured-data.design.md
- Analysis at: front-spotLine/docs/03-analysis/seo-structured-data.analysis.md

## Social Interactions Backend Analysis History
- Feature: social-interactions-backend
- v0.1 (2026-03-31): Match Rate **97%** -- 0 missing, 5 minor deviations, 6 beneficial extras
- 20/20 checklist items implemented: 7 entities, 7 repos, 3 services, 4 controllers, 9 DTOs, SecurityConfig
- Minor deviations: @JsonProperty for boolean is* fields, unused dep removal, parentRouteId logic, @ResponseStatus
- Beneficial extras: getCurrentEmail(), null-safe email, Math.max(0,...), @Transactional(readOnly=true)
- Analysis at: docs/03-analysis/social-interactions-backend.analysis.md

## User Profile Backend Analysis History
- Feature: user-profile-backend
- v0.1 (2026-03-31): Match Rate **100%** -- 0 missing, 0 deviations, 0 extras
- 7/7 checklist items: 3 DTOs, 2 repo methods, 1 service, 3 controller endpoints
- Verbatim match: all code identical to design document specifications
- Analysis at: docs/03-analysis/user-profile-backend.analysis.md

## Experience Posting Analysis History
- Feature: experience-posting
- v0.1 (2026-03-31): Match Rate **100%** -- 0 missing, 0 deviations, 0 extras
- 12/12 checklist items: 2 repo queries, 6 service methods (create/update/delete x2), 4 controller integrations, 2 user endpoints
- Verbatim match: all code identical to design document specifications
- Key pattern: verifyOwnership() with legacy null-check, bulkCreate delegates to create(req, null, "crew")
- Analysis at: docs/03-analysis/experience-posting.analysis.md

## Feed Discovery API Analysis History
- Feature: feed-discovery-api
- v0.1 (2026-04-01): Match Rate **100%** -- 0 missing, 0 deviations, 0 extras
- 10/10 checklist items (A1-C2): FeedSort enum, RoutePreviewResponse coverImageUrl, RouteRepo JPQL, SpotRepo 8 queries, SpotService findRoutesBySpotId+list sort, RouteService S3Service+coverImageUrl, SpotController+RouteController sort params
- Verbatim match: all code identical to design document specifications
- Key pattern: parseFeedSort() duplicated in both controllers (graceful fallback to POPULAR), resolveCoverImageUrl() static private in DTO
- Analysis at: docs/03-analysis/feed-discovery-api.analysis.md

## QR Partner System Backend Analysis History
- Feature: qr-partner-system-backend
- v0.1 (2026-04-01): Match Rate **100%** -- 0 missing, 0 deviations, 0 extras
- 25/25 checklist items: 2 enums, 3 entities, 3 repos, 3 request DTOs, 4 response DTOs, 1 service, 2 controllers, 2 security mods, 2 spot extensions, 2 tests + 1 test mod
- 18 new files + 5 modified files, 10 API endpoints, 12 repo methods, 13 service methods
- Verbatim match: all code identical to design document specifications
- Key additions: Partner/PartnerQrCode/QrScanLog entities, ROLE_ADMIN security, SpotDetailResponse partner field
- Analysis at: docs/03-analysis/qr-partner-system-backend.analysis.md

## Architecture Pattern
- Layers: controller/ -> service/ -> domain/(entity, enums, repository) + infrastructure/place/
- DTOs: dto/request/ + dto/response/ (separate from domain)
- Config: config/ (Security, CORS, Cache, GlobalExceptionHandler)
- Exceptions: exception/ (ResourceNotFoundException, ErrorResponse)
