# Comment System Analysis Report

> **Analysis Type**: Gap Analysis (Design vs Implementation)
>
> **Project**: Spotline Backend + Frontend
> **Analyst**: AI Assistant
> **Date**: 2026-04-03
> **Design Doc**: [comment-system.design.md](../02-design/features/comment-system.design.md)

---

## 1. Analysis Overview

### 1.1 Analysis Purpose

Design 문서(23단계 Implementation Order)와 실제 구현 코드의 일치율을 측정하고, API 스펙/데이터 모델/UI/Security/Test 차이를 파악한다.

### 1.2 Analysis Scope

- **Design Document**: `docs/02-design/features/comment-system.design.md`
- **Backend Path**: `src/main/java/com/spotline/api/` (8 new files + 5 modified files)
- **Frontend Path**: `front-spotLine/src/` (4 new files + 3 modified files)
- **Analysis Date**: 2026-04-03

---

## 2. Overall Scores

| Category | Score | Status |
|----------|:-----:|:------:|
| Design Match (Checklist) | 91% (21/23) | ⚠️ |
| API Spec Match | 95% | ⚠️ |
| Data Model Match | 100% | ✅ |
| UI Component Match | 100% | ✅ |
| Architecture Compliance | 100% | ✅ |
| Convention Compliance | 100% | ✅ |
| **Overall** | **91%** | **✅** |

---

## 3. Implementation Checklist Comparison (23 Items)

### 3.1 Backend (Items 1-15)

| # | Item | Design | Implementation | Status |
|---|------|--------|----------------|--------|
| 1 | `CommentTargetType` Enum | `SPOT, ROUTE` | `SPOT, ROUTE` | ✅ Verbatim |
| 2 | `Comment` Entity | 14 fields, 4 indexes, `@Builder` | 14 fields, 4 indexes, `@Builder` | ✅ Verbatim |
| 3 | `CommentRepository` | `findByTargetTypeAndTargetIdAndParentIsNull...` | Exact method + `countByTargetTypeAndTargetIdAndIsDeletedFalse` extra | ✅ |
| 4 | `CreateCommentRequest` DTO | `targetType`, `targetId`, `content`, `parentId` + validation | Identical with `@NotNull`, `@NotBlank`, `@Size(max=500)` | ✅ Verbatim |
| 5 | `UpdateCommentRequest` DTO | `content` + validation | Identical with `@NotBlank`, `@Size(max=500)` | ✅ Verbatim |
| 6 | `CommentResponse` DTO | `from(Comment)` with soft-delete content masking | Identical: `"삭제된 댓글입니다"` replacement, recursive replies mapping | ✅ Verbatim |
| 7 | `CommentService` | CRUD + validation + commentsCount sync | Identical logic: `validateTargetExists`, 1-depth check, owner check, `updateCommentsCount` | ✅ Verbatim |
| 8 | `CommentController` | 4 endpoints, `AuthUtil.requireUserId()` | 4 endpoints, `authUtil.requireUserId()` | ✅ |
| 9 | SecurityConfig: GET permitAll | `GET /api/v2/comments/**` permitAll | `.requestMatchers(HttpMethod.GET, "/api/v2/comments/**").permitAll()` | ✅ Verbatim |
| 10 | Spot Entity `commentsCount` | `@Builder.Default Integer commentsCount = 0` | Line 123: `@Builder.Default private Integer commentsCount = 0` | ✅ Verbatim |
| 11 | Route Entity `commentsCount` | `@Builder.Default Integer commentsCount = 0` | Line 74: `@Builder.Default private Integer commentsCount = 0` | ✅ Verbatim |
| 12 | SpotDetailResponse `commentsCount` | Include in response | Line 52: `private Integer commentsCount` + mapped in `from()` | ✅ Verbatim |
| 13 | RouteDetailResponse `commentsCount` | Include in response | Line 32: `private Integer commentsCount` + mapped in `from()` | ✅ Verbatim |
| 14 | CommentServiceTest (9 cases) | 9 test cases listed | **Not implemented** | ❌ Missing |
| 15 | CommentControllerTest (5 cases) | 5 test cases listed | **Not implemented** | ❌ Missing |

### 3.2 Frontend (Items 16-23)

| # | Item | Design | Implementation | Status |
|---|------|--------|----------------|--------|
| 16 | `types/index.ts` Comment types | `CommentTargetType`, `CommentResponse`, `CreateCommentRequest`, `UpdateCommentRequest` | All 4 types present at lines 627-655 | ✅ Verbatim |
| 17 | `api.ts` Comment API functions | `fetchComments`, `createComment`, `updateComment`, `deleteComment` | All 4 functions at lines 1101-1147, with auth headers | ✅ Verbatim |
| 18 | `CommentForm` component | Input + submit + auth check | 77 lines: `"use client"`, `useAuthStore`, `cn()`, 500 char limit, auth guard | ✅ |
| 19 | `CommentMenu` component | Dropdown with edit/delete | 53 lines: `MoreHorizontal` icon, `Pencil`/`Trash2`, click-outside close | ✅ |
| 20 | `CommentItem` component | Avatar + content + time + actions + replies | 178 lines: `formatRelativeTime`, reply toggle, edit inline, delete confirm, recursive replies | ✅ |
| 21 | `CommentSection` component | List + pagination + form | 148 lines: fetch/create/reply/update/delete handlers, "더 보기" pagination, empty state | ✅ |
| 22 | Spot page integration | `<CommentSection targetType="SPOT" ...>` | `page.tsx` line 120: `<CommentSection targetType="SPOT" targetId={spot.id} commentsCount={spot.commentsCount ?? 0} />` | ✅ Verbatim |
| 23 | Route page integration | `<CommentSection targetType="ROUTE" ...>` | `page.tsx` line 74: `<CommentSection targetType="ROUTE" targetId={route.id} commentsCount={route.commentsCount ?? 0} />` | ✅ Verbatim |

---

## 4. API Specification Comparison (4 Endpoints)

| Method | Path | Design | Implementation | Status |
|--------|------|--------|----------------|--------|
| GET | `/api/v2/comments` | Public, pagination (page/size, max 50) | `getComments()` with `Math.min(size, 50)` clamp | ✅ Match |
| POST | `/api/v2/comments` | Auth required, 201 Created | `@ResponseStatus(HttpStatus.CREATED)`, `authUtil.requireUserId()` | ✅ Match |
| PUT | `/api/v2/comments/{id}` | Owner only, 200 OK | Owner check in service, 200 default | ✅ Match |
| DELETE | `/api/v2/comments/{id}` | Owner only, soft delete, 200 OK with `{id, isDeleted}` body | `@ResponseStatus(HttpStatus.NO_CONTENT)`, `void` return | ⚠️ Deviation |

### 4.1 DELETE Response Deviation

| Aspect | Design | Implementation | Impact |
|--------|--------|----------------|--------|
| HTTP Status | 200 OK | 204 No Content | Low |
| Response Body | `{ "id": "uuid", "isDeleted": true }` | Empty body (void) | Low |

**Assessment**: 204 No Content is a common REST convention for DELETE operations and arguably better practice. The frontend `deleteComment()` function already handles this correctly (expects void). Low impact deviation -- implementation is acceptable.

---

## 5. Data Model Comparison

### 5.1 Comment Entity

| Field | Design | Implementation | Status |
|-------|--------|----------------|--------|
| id | `UUID, @GeneratedValue(UUID)` | Identical | ✅ |
| targetType | `@Enumerated(STRING), @Column(nullable=false)` | Identical | ✅ |
| targetId | `UUID, @Column(nullable=false)` | Identical | ✅ |
| parent | `@ManyToOne(LAZY), @JoinColumn("parent_id")` | Identical | ✅ |
| replies | `@OneToMany(mappedBy="parent", LAZY), @OrderBy("createdAt ASC")` | Identical | ✅ |
| userId | `String, @Column(nullable=false)` | Identical | ✅ |
| userName | `String, @Column(nullable=false)` | Identical | ✅ |
| userAvatarUrl | `String, nullable` | Identical | ✅ |
| content | `String, @Column(nullable=false, TEXT)` | Identical | ✅ |
| isDeleted | `Boolean, @Builder.Default = false` | Identical | ✅ |
| createdAt | `LocalDateTime, @CreationTimestamp` | Identical | ✅ |
| updatedAt | `LocalDateTime, @UpdateTimestamp` | Identical | ✅ |

### 5.2 Indexes

| Index Name | Design | Implementation | Status |
|------------|--------|----------------|--------|
| idx_comment_target | `(targetType, targetId)` | Identical | ✅ |
| idx_comment_parent | `(parent_id)` | Identical | ✅ |
| idx_comment_user | `(userId)` | Identical | ✅ |
| idx_comment_created | `(createdAt)` | Identical | ✅ |

### 5.3 Spot/Route Entity Changes

| Entity | Field | Design | Implementation | Status |
|--------|-------|--------|----------------|--------|
| Spot | commentsCount | `@Builder.Default Integer = 0` | Line 123 | ✅ |
| Route | commentsCount | `@Builder.Default Integer = 0` | Line 74 | ✅ |

**Data Model Match: 100%** -- All fields, types, annotations, and indexes are verbatim matches.

---

## 6. UI Component Comparison

| Component | Design Spec | Implementation | Status |
|-----------|-------------|----------------|--------|
| `CommentSection` | Container + data fetching + pagination | 148 lines, fetch/CRUD/reply handlers, "더 보기" button | ✅ |
| `CommentItem` | Avatar + name + content + time + actions | 178 lines, `formatRelativeTime`, edit inline, recursive replies | ✅ |
| `CommentForm` | Input + submit + auth guard | 77 lines, `"use client"`, auth check, 500 char limit | ✅ |
| `CommentMenu` | Dropdown with edit/delete (owner only) | 53 lines, `MoreHorizontal`/`Pencil`/`Trash2`, click-outside | ✅ |

### 6.1 Design Spec Features Implemented

| Feature | Design | Implementation | Status |
|---------|--------|----------------|--------|
| Relative time display | "2시간 전", "3일 전" | `formatRelativeTime()` in CommentItem | ✅ |
| Soft delete display | "삭제된 댓글입니다" | Content replacement in both backend DTO and frontend handler | ✅ |
| Reply toggle | "답글 N개 더 보기" | `showReplies` state with collapse/expand | ✅ |
| Non-auth user guard | "로그인 후 댓글 작성" | `isAuthenticated` check in CommentForm | ✅ |
| Optimistic UI | Immediate list update | `handleSubmit`/`handleReply`/`handleUpdate`/`handleDelete` in CommentSection | ✅ |
| Pagination | "더 보기" button | Page-based load more with totalPages check | ✅ |
| Edit inline | Textarea + save/cancel | `isEditing` state in CommentItem | ✅ |
| Delete confirm | Confirmation dialog | `confirm("댓글을 삭제하시겠습니까?")` | ✅ |
| `cn()` utility | Conditional classes | Used in CommentItem, CommentForm | ✅ |
| `"use client"` directive | All interactive components | All 4 components | ✅ |

---

## 7. Security Comparison

| Rule | Design | Implementation | Status |
|------|--------|----------------|--------|
| GET comments: Public | permitAll | SecurityConfig line 35: `GET /api/v2/comments/**` permitAll | ✅ |
| POST/PUT/DELETE: Auth required | authenticated | SecurityConfig lines 44-47: generic POST/PUT/DELETE authenticated | ✅ |
| Owner-only edit/delete | userId check | CommentService: `comment.getUserId().equals(userId)` | ✅ |
| Content length limit | 500 chars | `@Size(max=500)` on both DTOs + `maxLength={500}` on frontend textarea | ✅ |
| 1-depth reply limit | parentId validation | `parent.getParent() != null` check in createComment | ✅ |
| XSS prevention | React auto-escaping | TEXT storage + React rendering (no dangerouslySetInnerHTML) | ✅ |

---

## 8. Test Plan Comparison

| Design Test | Expected | Implemented | Status |
|-------------|----------|-------------|--------|
| **CommentServiceTest (9 cases)** | | | |
| Top-level comment creation + commentsCount | JUnit 5 + Mockito | Not found | ❌ |
| Reply creation + parent link | JUnit 5 + Mockito | Not found | ❌ |
| Reply-to-reply rejection (400) | JUnit 5 + Mockito | Not found | ❌ |
| Other user edit rejection (403) | JUnit 5 + Mockito | Not found | ❌ |
| Other user delete rejection (403) | JUnit 5 + Mockito | Not found | ❌ |
| Soft delete display check | JUnit 5 + Mockito | Not found | ❌ |
| Delete commentsCount decrement | JUnit 5 + Mockito | Not found | ❌ |
| Non-existent target (404) | JUnit 5 + Mockito | Not found | ❌ |
| Pagination (page/size) | JUnit 5 + Mockito | Not found | ❌ |
| **CommentControllerTest (5 cases)** | | | |
| GET unauthenticated allowed | @WebMvcTest | Not found | ❌ |
| POST unauthenticated 401 | @WebMvcTest | Not found | ❌ |
| POST authenticated 201 | @WebMvcTest | Not found | ❌ |
| PUT owner 200 | @WebMvcTest | Not found | ❌ |
| DELETE owner 200 | @WebMvcTest | Not found | ❌ |

**Test Coverage: 0/14 cases (0%)** -- All designed tests are missing.

---

## 9. Beneficial Extras (Design X, Implementation O)

| # | Item | Location | Description |
|---|------|----------|-------------|
| 1 | `countByTargetTypeAndTargetIdAndIsDeletedFalse` | CommentRepository | Extra query for accurate count (not in design) |
| 2 | `Math.max(0, ...)` guard on commentsCount | CommentService L118, L122 | Prevents negative count |
| 3 | Idempotent delete | CommentService L93-94 | `if (comment.getIsDeleted()) return;` -- silent no-op |
| 4 | Reply collapse/expand | CommentItem L145-173 | "답글 N개 보기" / "답글 접기" toggle (design shows "답글 2개 더 보기" but no collapse) |

---

## 10. Minor Deviations

| # | Item | Design | Implementation | Impact |
|---|------|--------|----------------|--------|
| 1 | DELETE response | 200 + `{id, isDeleted}` body | 204 No Content + void | Low -- better REST practice |
| 2 | DELETE return type | `CommentResponse`-like | `void` | Low -- frontend already handles void |

---

## 11. Match Rate Calculation

```
Implementation Checklist: 21/23 items implemented (91.3%)

Breakdown:
  Backend items 1-13:  13/13 = 100%
  Backend tests 14-15:  0/2  =   0%  (MISSING)
  Frontend items 16-23: 8/8  = 100%

API Endpoints: 4/4 implemented (1 minor deviation)
Data Model: 100% verbatim match
UI Components: 4/4 implemented with all design features
Security: 100% match
Tests: 0/14 cases implemented

Overall Match Rate: 91%
```

```
+---------------------------------------------+
|  Overall Match Rate: 91%                     |
+---------------------------------------------+
|  Backend Code:        13/13 items  (100%)    |
|  Frontend Code:        8/8  items  (100%)    |
|  Tests:                0/2  items  (  0%)    |
|  API Deviation:        1 minor               |
+---------------------------------------------+
```

---

## 12. Recommended Actions

### 12.1 Immediate (Match Rate -> 100%)

| Priority | Item | Location | Description |
|----------|------|----------|-------------|
| 1 | Write `CommentServiceTest` | `src/test/java/.../service/CommentServiceTest.java` | 9 test cases as specified in design Section 8.2 |
| 2 | Write `CommentControllerTest` | `src/test/java/.../controller/CommentControllerTest.java` | 5 test cases as specified in design Section 8.2 |

### 12.2 Optional Design Doc Sync

| Item | Description | Action |
|------|-------------|--------|
| DELETE response | Design says 200 + body, impl is 204 + void | Update design to reflect 204 No Content (preferred) |

---

## 13. Conclusion

Match Rate >= 90% threshold met. All functional code (entities, DTOs, service, controller, frontend components, page integrations, security config) is implemented verbatim to design specification. The only gap is the test files (14 test cases), which account for the 9% shortfall. The single API deviation (DELETE 204 vs 200) is an improvement over the design.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial analysis | AI Assistant |
