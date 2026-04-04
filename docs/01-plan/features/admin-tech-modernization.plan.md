# Admin Tech Modernization Planning Document

> **Summary**: admin-spotLine 기술 스택을 front-spotLine과 동일 수준으로 현대화
>
> **Project**: admin-spotLine
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | admin-spotLine이 React 18 + Vite 4 + Tailwind 3 + ESLint 8 + react-query 3을 사용하여 front-spotLine(React 19 + Tailwind 4 + ESLint 9)과 기술 스택 불일치. ESLint 설정이 .jsx만 검사하여 TypeScript 파일 미검사. |
| **Solution** | React 19, Vite 6, Tailwind CSS 4, ESLint 9 flat config, @tanstack/react-query 5로 일괄 업그레이드. 빌드 파이프라인 정비. |
| **Function/UX Effect** | 개발자 경험 개선 — 두 레포 간 일관된 도구 사용, 빠른 빌드, 더 나은 타입 검사. 사용자에게는 변화 없음 (내부 리팩터링). |
| **Core Value** | 기술 부채 해소 — 프로젝트 전체 기술 스택 통일로 유지보수성 향상, 보안 패치 최신화, 향후 기능 개발 가속화. |

---

## 1. Overview

### 1.1 Purpose

admin-spotLine의 핵심 의존성(React, Vite, Tailwind, ESLint, React Query)을 최신 버전으로 업그레이드하여 front-spotLine과 기술 스택을 통일한다. 빌드 설정과 린트 설정을 정비하여 코드 품질을 높인다.

### 1.2 Background

| 패키지 | admin (현재) | front (현재) | 업그레이드 목표 |
|---------|-------------|-------------|----------------|
| React | 18.2.0 | 19.2.3 | 19.x |
| React DOM | 18.2.0 | 19.2.3 | 19.x |
| Vite | 4.5.0 | — (Next.js) | 6.x |
| Tailwind CSS | 3.3.6 | 4.x | 4.x |
| ESLint | 8.53.0 | 9.x | 9.x |
| React Query | 3.39.3 (react-query) | — | @tanstack/react-query 5.x |
| TypeScript | (implicit) | 5.x | 5.x (explicit) |

### 1.3 현재 문제점

1. **ESLint 미작동**: lint 스크립트가 `--ext js,jsx`만 검사 → `.tsx` 파일 미검사
2. **react-query 3**: deprecated 패키지명 (`react-query` → `@tanstack/react-query`)
3. **Tailwind 3**: front-spotLine과 다른 설정 방식 (PostCSS vs CSS-first)
4. **Vite 4**: 2세대 이전 버전, 빌드 성능 및 보안 패치 부재
5. **React 18**: React 19의 ref/compiler 개선 미사용

---

## 2. Scope

### 2.1 In Scope

- [ ] React 18 → 19 업그레이드 (react, react-dom, @types/react, @types/react-dom)
- [ ] Vite 4 → 6 업그레이드 (@vitejs/plugin-react 포함)
- [ ] Tailwind CSS 3 → 4 마이그레이션 (postcss.config.js, tailwind.config.js 제거)
- [ ] ESLint 8 → 9 마이그레이션 (flat config, .tsx 검사 포함)
- [ ] react-query 3 → @tanstack/react-query 5 마이그레이션
- [ ] TypeScript 5.x 명시적 devDependency 추가
- [ ] package.json scripts 정비 (lint, type-check 추가)
- [ ] 빌드 검증 (vite build 성공 확인)

### 2.2 Out of Scope

- 기능 변경/추가 (순수 기술 스택 업그레이드)
- 컴포넌트 리팩터링 (any 타입 제거 등은 별도 피처)
- Storybook, Prettier, husky 추가 (별도 피처)
- React Router 업그레이드 (6.x 유지, 호환성 문제 없음)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | React 19 + React DOM 19 업그레이드, @types 업데이트 | High | Pending |
| FR-02 | Vite 6 + @vitejs/plugin-react 최신 업그레이드 | High | Pending |
| FR-03 | Tailwind CSS 4 마이그레이션 — CSS-first 방식, config 파일 제거 | High | Pending |
| FR-04 | ESLint 9 flat config 마이그레이션 — .tsx/.ts 검사 포함 | High | Pending |
| FR-05 | react-query 3 → @tanstack/react-query 5 마이그레이션 | High | Pending |
| FR-06 | TypeScript 5.x devDependency 추가, type-check 스크립트 추가 | Medium | Pending |
| FR-07 | 빌드 성공 + lint 통과 + type-check 통과 검증 | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement |
|----------|----------|-------------|
| Compatibility | 기존 기능 100% 동작 유지 | 빌드 성공 + 주요 페이지 렌더링 확인 |
| Performance | 빌드 시간 동일 또는 개선 | vite build 소요 시간 비교 |
| Code Quality | ESLint 에러 0개 (warning 허용) | `pnpm lint` 결과 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] `pnpm build` 성공 (0 errors)
- [ ] `pnpm lint` 통과 (0 errors)
- [ ] `pnpm type-check` 통과 (0 errors)
- [ ] 모든 import 경로 정상 (react-query → @tanstack/react-query)
- [ ] Gap Analysis 90% 이상

### 4.2 Quality Criteria

- [ ] 기존 페이지 (Dashboard, Spots, Routes, Curation, ModerationQueue) 정상 빌드
- [ ] Tailwind 클래스 모두 정상 적용
- [ ] Chart.tsx (recharts) 정상 동작

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| React 19 breaking changes (forwardRef 제거 등) | Medium | Low | Admin은 forwardRef 거의 미사용, 빌드 시 에러 즉시 확인 |
| Tailwind 4 클래스 호환성 | Medium | Medium | Tailwind 4 upgrade guide 참조, 커스텀 설정 최소 (기본 preset 사용) |
| @tanstack/react-query 5 API 변경 | High | High | useMutation/useQuery 시그니처 변경 — 전체 파일 검색+일괄 수정 |
| ESLint 9 flat config 전환 복잡도 | Low | Low | 단순 설정, 플러그인 적음 |
| Chart.tsx 타입 에러 | Medium | Medium | recharts는 React 19 호환, Chart.tsx 타입 any 다수 — 빌드 시 확인 |

---

## 6. Implementation Items

| # | Item | Files | Description |
|:-:|------|-------|-------------|
| 1 | package.json 의존성 업그레이드 | `package.json` | react 19, vite 6, tailwind 4, eslint 9, @tanstack/react-query 5, typescript 5 |
| 2 | pnpm install + lock file 갱신 | `pnpm-lock.yaml` | 의존성 설치 |
| 3 | vite.config.ts 업데이트 | `vite.config.ts` | Vite 6 호환 설정 |
| 4 | Tailwind CSS 4 마이그레이션 | `tailwind.config.js` 삭제, `postcss.config.js` 삭제, `src/index.css` 수정 | CSS-first 방식 전환 |
| 5 | ESLint 9 flat config | `.eslintrc*` 삭제, `eslint.config.js` 생성 | flat config + TypeScript 검사 |
| 6 | react-query → @tanstack/react-query 임포트 수정 | 전체 `.tsx` 파일 | `import { useQuery } from "react-query"` → `import { useQuery } from "@tanstack/react-query"` |
| 7 | @tanstack/react-query 5 API 변경 적용 | 전체 `.tsx` 파일 | useMutation 시그니처 변경, QueryClientProvider 설정 |
| 8 | package.json scripts 정비 | `package.json` | lint, type-check 스크립트 추가/수정 |
| 9 | 빌드 + lint + type-check 검증 | — | 전체 통과 확인 |

---

## 7. Architecture Considerations

### 7.1 Project Level

Dynamic — admin-only 변경, 기존 아키텍처 유지

### 7.2 Key Decisions

| Decision | Selected | Rationale |
|----------|----------|-----------|
| React Query 버전 | @tanstack/react-query 5 | 최신 안정 버전, React 19 호환 |
| Tailwind 4 설정 | CSS-first (@import) | front-spotLine과 동일 방식 |
| ESLint 설정 | eslint.config.js (flat) | ESLint 9 표준, .tsx 검사 포함 |
| 패키지 매니저 | pnpm (기존 유지) | front-spotLine과 동일 |

---

## 8. Next Steps

1. [ ] Design 문서 작성 (`/pdca design admin-tech-modernization`)
2. [ ] 구현 (`/pdca do admin-tech-modernization`)
3. [ ] Gap Analysis (`/pdca analyze admin-tech-modernization`)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
