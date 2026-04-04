# Admin Tech Modernization Design Document

> **Summary**: admin-spotLine 기술 스택을 React 19 + Vite 6 + Tailwind 4 + ESLint 9 + @tanstack/react-query 5로 현대화
>
> **Project**: admin-spotLine
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft
> **Planning Doc**: [admin-tech-modernization.plan.md](../../01-plan/features/admin-tech-modernization.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- front-spotLine과 동일 수준의 기술 스택 통일
- ESLint가 `.tsx/.ts` 파일을 정상 검사하도록 수정
- 빌드 파이프라인 정비 (lint + type-check + build 모두 통과)
- 기존 기능 100% 유지 (순수 기술 업그레이드)

### 1.2 Design Principles

- **Zero Feature Change**: 사용자 기능 변경 없이 내부 의존성만 업그레이드
- **Consistency**: front-spotLine과 동일한 도구 체인 유지
- **Incremental Verification**: 각 단계별 빌드 검증으로 문제 조기 발견

---

## 2. Migration Specifications

### 2.1 Version Matrix

| Package | Current | Target | Breaking Changes |
|---------|---------|--------|-----------------|
| react | 18.2.0 | ^19.0.0 | forwardRef deprecated (미사용), ref as prop |
| react-dom | 18.2.0 | ^19.0.0 | ReactDOM.render 제거 (createRoot 사용 중 — OK) |
| @types/react | 18.2.37 | ^19.0.0 | 타입 시그니처 변경 |
| @types/react-dom | 18.2.15 | ^19.0.0 | 타입 시그니처 변경 |
| vite | 4.5.0 | ^6.0.0 | Node 18+ 필수, config API 호환 |
| @vitejs/plugin-react | (current) | ^4.4.0 | Vite 6 호환 버전 |
| tailwindcss | 3.3.6 | ^4.0.0 | CSS-first config, PostCSS 불필요 |
| eslint | 8.53.0 | ^9.0.0 | flat config 필수 |
| react-query | 3.39.3 | — (제거) | @tanstack/react-query로 교체 |
| @tanstack/react-query | — (신규) | ^5.0.0 | useMutation 시그니처 변경 |
| typescript | (implicit) | ^5.7.0 | devDependency 명시 추가 |
| autoprefixer | 10.4.16 | — (제거) | Tailwind 4에서 내장 |
| postcss | 8.4.31 | — (제거) | Tailwind 4에서 불필요 |

### 2.2 Dependencies to Remove

```
react-query
autoprefixer
postcss
```

### 2.3 Dependencies to Add

```
@tanstack/react-query@^5
typescript@^5.7
@eslint/js
typescript-eslint
globals
```

---

## 3. Detailed Migration Plan

### Item 1: package.json 의존성 업그레이드

**File**: `package.json`

변경 사항:
```json
{
  "dependencies": {
    "react": "^19.0.0",           // was 18.2.0
    "react-dom": "^19.0.0",       // was 18.2.0
    "@tanstack/react-query": "^5.0.0"  // NEW (replaces react-query)
    // REMOVE: "react-query": "3.39.3"
  },
  "devDependencies": {
    "vite": "^6.0.0",             // was 4.5.0
    "@vitejs/plugin-react": "^4.4.0",  // update
    "tailwindcss": "^4.0.0",      // was 3.3.6
    "@tailwindcss/vite": "^4.0.0",     // NEW (Vite plugin)
    "eslint": "^9.0.0",           // was 8.53.0
    "@eslint/js": "^9.0.0",       // NEW
    "typescript-eslint": "^8.0.0", // NEW
    "globals": "^16.0.0",         // NEW
    "typescript": "^5.7.0",       // NEW (explicit)
    "@types/react": "^19.0.0",    // was 18.2.37
    "@types/react-dom": "^19.0.0" // was 18.2.15
    // REMOVE: autoprefixer, postcss
    // REMOVE: eslint-plugin-react-hooks, eslint-plugin-react-refresh (old versions)
  }
}
```

### Item 2: pnpm install + lock file

```bash
# 1. 기존 node_modules + lockfile 삭제
rm -rf node_modules pnpm-lock.yaml

# 2. 재설치
pnpm install
```

### Item 3: vite.config.ts 업데이트

**File**: `vite.config.ts`

현재:
```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: { port: 3004, ... },
});
```

변경:
```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 3004, ... },
});
```

변경점: `@tailwindcss/vite` 플러그인 추가 (Tailwind 4 Vite 통합)

### Item 4: Tailwind CSS 4 마이그레이션

**삭제 파일**:
- `tailwind.config.js`
- `postcss.config.js`

**수정 파일**: `src/index.css`

현재:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  * { @apply border-gray-200; }
  body { @apply bg-white text-gray-900; }
}
```

변경:
```css
@import "tailwindcss";

@theme {
  --color-primary-50: #f0f9ff;
  --color-primary-500: #3b82f6;
  --color-primary-600: #2563eb;
  --color-primary-700: #1d4ed8;
}

@layer base {
  * { @apply border-gray-200; }
  body { @apply bg-white text-gray-900; }
}
```

핵심 변경:
- `@tailwind` 디렉티브 → `@import "tailwindcss"`
- `tailwind.config.js` 커스텀 색상 → `@theme` 블록의 CSS custom properties
- PostCSS 의존성 제거 (Vite 플러그인으로 대체)

### Item 5: ESLint 9 flat config 마이그레이션

**삭제 파일**: `.eslintrc.cjs` (또는 `.eslintrc.json` 등 기존 설정)

**신규 파일**: `eslint.config.js`

```javascript
import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";

export default tseslint.config(
  { ignores: ["dist"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
    },
  }
);
```

핵심 변경:
- `.eslintrc*` → `eslint.config.js` (flat config)
- `--ext js,jsx` → `**/*.{ts,tsx}` (TypeScript 파일 검사 포함)
- `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` 최신 버전 필요

### Item 6: react-query → @tanstack/react-query 임포트 수정

**대상 파일** (20개):

모든 `from "react-query"` 및 `from 'react-query'` 임포트를 일괄 변경:

```typescript
// BEFORE
import { useQuery, useMutation, useQueryClient } from "react-query";
import { QueryClient, QueryClientProvider } from "react-query";

// AFTER
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
```

검색 패턴: `from ["']react-query["']` → `from "@tanstack/react-query"`

### Item 7: @tanstack/react-query 5 API 변경 적용

**useMutation 시그니처 변경** (13+ 파일):

@tanstack/react-query 5는 positional arguments를 제거하고 object syntax만 지원.

**패턴 A** (이미 호환 — 변경 불필요):
```typescript
// Object syntax — v5 호환
useMutation({
  mutationFn: async (data) => { ... },
  onSuccess: () => { ... },
});
```

**패턴 B** (변경 필요):
```typescript
// BEFORE: positional syntax (v3)
useMutation(
  async (id: string) => { await api.delete(id); },
  { onSuccess: () => { queryClient.invalidateQueries("key"); } }
);

// AFTER: object syntax (v5)
useMutation({
  mutationFn: async (id: string) => { await api.delete(id); },
  onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["key"] }); },
});
```

**useQuery 시그니처 변경**:
```typescript
// BEFORE (v3)
useQuery("key", fetchFn);
useQuery(["key", param], fetchFn);

// AFTER (v5)
useQuery({ queryKey: ["key"], queryFn: fetchFn });
useQuery({ queryKey: ["key", param], queryFn: fetchFn });
```

**invalidateQueries 변경**:
```typescript
// BEFORE (v3)
queryClient.invalidateQueries("key");
queryClient.invalidateQueries(["key"]);

// AFTER (v5)
queryClient.invalidateQueries({ queryKey: ["key"] });
```

**대상 파일 및 패턴**:

| File | useMutation Style | useQuery | Changes Needed |
|------|------------------|----------|----------------|
| ModerationQueue.tsx | positional | positional | Both |
| SpotManagement.tsx | positional | positional | Both |
| Stores.tsx | positional | positional | Both |
| RouteManagement.tsx | positional | positional | Both |
| Curation.tsx | positional | positional | Both |
| PartnerRegistration.tsx | object | positional | useQuery only |
| PartnerDetail.tsx | object | positional | useQuery only |
| QRCodeManager.tsx | object | positional | useQuery only |
| Dashboard.tsx | — | positional | useQuery only |
| Admins.tsx | both | positional | Both |
| SpotDetail.tsx | positional | positional | Both |
| ContentModeration.tsx | positional | positional | Both |
| App.tsx | — | — | QueryClient config |

### Item 8: package.json scripts 정비

**File**: `package.json`

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint .",
    "type-check": "tsc --noEmit"
  }
}
```

변경점:
- `lint`: `eslint . --ext js,jsx` → `eslint .` (flat config가 파일 패턴 관리)
- `type-check`: 신규 추가
- `build`: `tsc -b` 추가로 타입 검사 포함

### Item 9: 빌드 + lint + type-check 검증

검증 순서:
1. `pnpm lint` — ESLint 에러 0개 확인
2. `pnpm type-check` — TypeScript 에러 0개 확인
3. `pnpm build` — Vite 빌드 성공 확인

---

## 4. Implementation Order

### 4.1 Implementation Checklist

| # | Item | Files | Estimated Changes |
|:-:|------|-------|-------------------|
| 1 | package.json 의존성 업그레이드 | `package.json` | 15+ line changes |
| 2 | pnpm install + lock file 갱신 | `pnpm-lock.yaml` | auto-generated |
| 3 | vite.config.ts 업데이트 | `vite.config.ts` | +2 lines |
| 4 | Tailwind CSS 4 마이그레이션 | `tailwind.config.js` 삭제, `postcss.config.js` 삭제, `src/index.css` | 3 files |
| 5 | ESLint 9 flat config | `.eslintrc*` 삭제, `eslint.config.js` 생성 | 2 files |
| 6 | react-query → @tanstack/react-query import | 20 .tsx files | 20 files bulk replace |
| 7 | @tanstack/react-query 5 API 변경 | 13+ .tsx files | ~50 useMutation + useQuery changes |
| 8 | package.json scripts 정비 | `package.json` | 3 lines |
| 9 | 빌드 + lint + type-check 검증 | — | verification only |

### 4.2 Execution Strategy

1. Items 1-2: 의존성 설치 (모든 변경의 기반)
2. Items 3-5: 빌드 도구 설정 (Vite, Tailwind, ESLint)
3. Items 6-7: 코드 변경 (react-query 마이그레이션)
4. Item 8: 스크립트 정비
5. Item 9: 전체 검증

---

## 5. Risk Mitigation

| Risk | Strategy |
|------|----------|
| Tailwind 4 custom color 누락 | `@theme` 블록에 primary 색상 4개 (50, 500, 600, 700) 명시 |
| useMutation 패턴 혼재 | positional → object 일괄 변환, 기존 object 패턴은 유지 |
| useQuery string key | 모든 string key를 array로 변환 (`"key"` → `["key"]`) |
| ESLint any 경고 폭발 | `@typescript-eslint/no-explicit-any: "warn"` 설정으로 에러가 아닌 경고 처리 |
| recharts 호환성 | React 19 호환 확인됨, Chart.tsx any 타입은 warn 허용 |

---

## 6. Success Criteria

- [ ] `pnpm build` — 0 errors
- [ ] `pnpm lint` — 0 errors (warnings 허용)
- [ ] `pnpm type-check` — 0 errors
- [ ] 모든 import: `@tanstack/react-query` (react-query 잔여 없음)
- [ ] ESLint가 `.tsx` 파일 검사 확인

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
