# Content Moderation Workflow Design Document

> **Summary**: 댓글 신고 API + Admin 모더레이션 큐 + Frontend 신고 버튼 — 콘텐츠 품질 관리 워크플로우
>
> **Project**: Spotline Backend + Admin + Frontend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft
> **Planning Doc**: [content-moderation-workflow.plan.md](../01-plan/features/content-moderation-workflow.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- 기존 Comment 시스템 패턴(targetType + targetId) 재사용
- 사용자 신고 → Admin 검토 → 조치의 3단계 워크플로우
- Backend: 신규 엔티티/서비스/컨트롤러 (기존 코드 수정 최소화)
- Admin: 새 페이지 1개 + Layout 사이드바 추가
- Frontend: CommentItem에 신고 버튼 + ReportModal 추가

### 1.2 Design Principles

- 다형적 신고: `targetType` + `targetId`로 향후 Spot/Route 신고 확장 가능
- 중복 신고 차단: 같은 사용자 + 같은 대상 조합에 unique constraint
- 모더레이션 액션: 기존 Comment.isDeleted 활용 (새 필드 불필요)

---

## 2. Architecture

### 2.1 Data Flow

```
[사용자 신고]
CommentItem → "신고" 버튼 클릭
  → ReportModal 열림 (사유 선택 + 상세 입력)
  → POST /api/v2/reports { targetType, targetId, reason, description }
  → ContentReportService.create() → 중복 체크 → save
  → 201 Created

[Admin 모더레이션]
Admin ModerationQueue 페이지 접근
  → GET /api/v2/admin/reports?status=PENDING&page=0
  → 신고 목록 표시 (신고자, 대상 댓글 내용, 사유, 날짜)
  → Admin이 신고 선택 → 조치 결정
  → PUT /api/v2/admin/reports/{id}/resolve { action, moderatorNote }
    → action=HIDE_CONTENT: Comment.isDeleted = true + report.status = RESOLVED
    → action=DISMISS: report.status = DISMISSED
  → 목록 갱신

[Dashboard 미처리 건수]
Admin Layout 사이드바
  → GET /api/v2/admin/reports/pending-count
  → "모더레이션" 메뉴 옆에 배지 표시
```

---

## 3. Backend Changes

### 3.1 ReportReason Enum — NEW

**File**: `domain/enums/ReportReason.java`

```java
public enum ReportReason {
    SPAM,
    INAPPROPRIATE,
    HARASSMENT,
    OTHER
}
```

### 3.2 ReportStatus Enum — NEW

**File**: `domain/enums/ReportStatus.java`

```java
public enum ReportStatus {
    PENDING,
    RESOLVED,
    DISMISSED
}
```

### 3.3 ModerationAction Enum — NEW

**File**: `domain/enums/ModerationAction.java`

```java
public enum ModerationAction {
    HIDE_CONTENT,
    DISMISS
}
```

### 3.4 ContentReport Entity — NEW

**File**: `domain/entity/ContentReport.java`

```java
@Entity
@Table(name = "content_reports", indexes = {
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_target", columnList = "targetType, targetId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_report_user_target",
        columnNames = {"reporterUserId", "targetType", "targetId"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentTargetType targetType;  // SPOT, ROUTE (기존 enum 재사용)

    @Column(nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private ModerationAction action;

    private String resolvedByAdminId;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String moderatorNote;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**설계 결정**: `targetType`에 기존 `CommentTargetType`(SPOT, ROUTE)을 직접 재사용하지 않고, 신고 대상을 구분하는 별도 enum이 필요할 수 있으나, 현재 스코프가 댓글 신고만이므로 `targetType`은 "COMMENT" 고정값으로 String 처리. 향후 Spot/Route 직접 신고 시 enum 확장.

**수정**: `targetType`을 String으로 변경하여 단순화:

```java
@Column(nullable = false)
private String targetType;  // "COMMENT" (향후 "SPOT", "ROUTE" 확장)
```

### 3.5 ContentReportRepository — NEW

**File**: `domain/repository/ContentReportRepository.java`

```java
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

    boolean existsByReporterUserIdAndTargetTypeAndTargetId(
        String reporterUserId, String targetType, UUID targetId);

    Page<ContentReport> findByStatusOrderByCreatedAtDesc(
        ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
```

### 3.6 DTOs — NEW

**File**: `dto/request/CreateReportRequest.java`

```java
@Data
public class CreateReportRequest {
    @NotNull
    private UUID targetId;

    @NotBlank
    private String targetType;  // "COMMENT"

    @NotNull
    private ReportReason reason;

    private String description;  // 선택적 상세 사유
}
```

**File**: `dto/request/ResolveReportRequest.java`

```java
@Data
public class ResolveReportRequest {
    @NotNull
    private ModerationAction action;  // HIDE_CONTENT or DISMISS

    private String moderatorNote;
}
```

**File**: `dto/response/ReportResponse.java`

```java
@Data @Builder
public class ReportResponse {
    private UUID id;
    private String reporterUserId;
    private String targetType;
    private UUID targetId;
    private String targetContent;     // 신고 대상 댓글 내용 (Admin용)
    private String targetUserName;    // 댓글 작성자 이름
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private ModerationAction action;
    private String moderatorNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static ReportResponse from(ContentReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterUserId(report.getReporterUserId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .action(report.getAction())
                .moderatorNote(report.getModeratorNote())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public static ReportResponse from(ContentReport report, Comment comment) {
        ReportResponse response = from(report);
        if (comment != null) {
            response.setTargetContent(comment.getContent());
            response.setTargetUserName(comment.getUserName());
        }
        return response;
    }
}
```

### 3.7 ContentReportService — NEW

**File**: `service/ContentReportService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentReportService {

    private final ContentReportRepository reportRepository;
    private final CommentRepository commentRepository;

    /** 신고 접수 */
    @Transactional
    public ReportResponse create(String userId, CreateReportRequest request) {
        // 중복 신고 체크
        if (reportRepository.existsByReporterUserIdAndTargetTypeAndTargetId(
                userId, request.getTargetType(), request.getTargetId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 콘텐츠입니다");
        }

        // 대상 존재 확인 (COMMENT인 경우)
        if ("COMMENT".equals(request.getTargetType())) {
            commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        }

        ContentReport report = ContentReport.builder()
                .reporterUserId(userId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    /** 신고 목록 조회 (Admin) */
    public Page<ReportResponse> list(ReportStatus status, Pageable pageable) {
        Page<ContentReport> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status != null ? status : ReportStatus.PENDING, pageable);

        return reports.map(report -> {
            if ("COMMENT".equals(report.getTargetType())) {
                Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                return ReportResponse.from(report, comment);
            }
            return ReportResponse.from(report);
        });
    }

    /** 미처리 신고 건수 */
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /** 신고 처리 (Admin) */
    @Transactional
    public ReportResponse resolve(UUID reportId, String adminId, ResolveReportRequest request) {
        ContentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId.toString()));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 신고입니다");
        }

        // HIDE_CONTENT 액션: 댓글 soft delete
        if (request.getAction() == ModerationAction.HIDE_CONTENT
                && "COMMENT".equals(report.getTargetType())) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
                comment.setIsDeleted(true);
                commentRepository.save(comment);
            });
        }

        report.setStatus(request.getAction() == ModerationAction.DISMISS
                ? ReportStatus.DISMISSED : ReportStatus.RESOLVED);
        report.setAction(request.getAction());
        report.setResolvedByAdminId(adminId);
        report.setResolvedAt(LocalDateTime.now());
        report.setModeratorNote(request.getModeratorNote());

        return ReportResponse.from(reportRepository.save(report));
    }
}
```

### 3.8 ContentReportController — NEW

**File**: `controller/ContentReportController.java`

```java
@RestController
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService reportService;
    private final AuthUtil authUtil;

    // ---- 사용자 API (/api/v2/reports) ----

    @PostMapping("/api/v2/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody CreateReportRequest request) {
        return reportService.create(authUtil.requireUserId(), request);
    }

    // ---- Admin API (/api/v2/admin/reports) ----

    @GetMapping("/api/v2/admin/reports")
    public Page<ReportResponse> list(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return reportService.list(status, pageable);
    }

    @GetMapping("/api/v2/admin/reports/pending-count")
    public Map<String, Long> getPendingCount() {
        return Map.of("count", reportService.getPendingCount());
    }

    @PutMapping("/api/v2/admin/reports/{id}/resolve")
    public ReportResponse resolve(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request) {
        return reportService.resolve(id, authUtil.requireUserId(), request);
    }
}
```

**Security**: `POST /api/v2/reports`는 `authenticated()` 규칙에 매핑. `GET/PUT /api/v2/admin/**`는 `hasRole("ADMIN")` 규칙에 매핑. 기존 SecurityConfig 변경 불필요.

---

## 4. Admin Changes

### 4.1 reportAPI — NEW

**File**: `admin-spotLine/src/services/v2/reportAPI.ts`

```typescript
import { apiClient } from "../base/apiClient";
import type { SpringPage } from "../../types/v2";

export interface ReportResponse {
  id: string;
  reporterUserId: string;
  targetType: string;
  targetId: string;
  targetContent: string | null;
  targetUserName: string | null;
  reason: "SPAM" | "INAPPROPRIATE" | "HARASSMENT" | "OTHER";
  description: string | null;
  status: "PENDING" | "RESOLVED" | "DISMISSED";
  action: "HIDE_CONTENT" | "DISMISS" | null;
  moderatorNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export const reportAPI = {
  getList: (params: { status?: string; page?: number; size?: number } = {}) => {
    const { page = 1, size = 20, ...rest } = params;
    return apiClient.get<SpringPage<ReportResponse>>("/api/v2/admin/reports", {
      params: { page: page - 1, size, ...rest },
    });
  },

  getPendingCount: () =>
    apiClient.get<{ count: number }>("/api/v2/admin/reports/pending-count"),

  resolve: (id: string, data: { action: "HIDE_CONTENT" | "DISMISS"; moderatorNote?: string }) =>
    apiClient.put<ReportResponse>(`/api/v2/admin/reports/${id}/resolve`, data),
};
```

### 4.2 ModerationQueue 페이지 — NEW

**File**: `admin-spotLine/src/pages/ModerationQueue.tsx`

```typescript
export default function ModerationQueue() {
  const [statusFilter, setStatusFilter] = useState<string>("PENDING");
  const [page, setPage] = useState(1);

  const { data, isLoading } = useQuery({
    queryKey: ["reports", statusFilter, page],
    queryFn: () => reportAPI.getList({ status: statusFilter, page }),
    select: (res) => res.data,
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResolveRequest }) =>
      reportAPI.resolve(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["reports"] }),
  });

  // 신고 목록 테이블: 신고자, 대상 댓글, 사유, 날짜, 상태, 액션 버튼
  // PENDING 상태: "숨김" + "기각" 버튼
  // RESOLVED/DISMISSED: 처리 결과 표시
}
```

**UI 구성**:
- 상단: 상태 필터 탭 (전체 / 미처리 / 처리됨 / 기각)
- 테이블: 신고 날짜, 대상 댓글 내용(truncated), 작성자, 사유, 상태
- 액션: PENDING 신고에 "숨김 처리" / "기각" 버튼 + moderatorNote 입력 모달

### 4.3 App.tsx — 라우트 등록

기존 `partners/:id` 아래에 추가:

```tsx
import ModerationQueue from "./pages/ModerationQueue";

<Route path="moderation" element={
  <ProtectedRoute requiredRole="admin"><ModerationQueue /></ProtectedRoute>
} />
```

### 4.4 Layout.tsx — 사이드바 메뉴 추가

`navigation` 배열에 추가 (시스템 섹션):

```typescript
import { Shield } from "lucide-react";

{ name: "모더레이션", href: "/moderation", icon: Shield, section: "system", minRole: "admin" },
```

미처리 건수 배지는 `NavLink` 컴포넌트 확장 또는 별도 `usePendingCount` 훅으로 구현:

```typescript
// Layout 내에서
const { data: pendingData } = useQuery({
  queryKey: ["reports-pending-count"],
  queryFn: () => reportAPI.getPendingCount(),
  select: (res) => res.data.count,
  refetchInterval: 60000, // 1분마다 갱신
});
```

NavLink 옆에 배지:
```tsx
{item.href === "/moderation" && pendingCount > 0 && (
  <span className="ml-auto rounded-full bg-red-500 px-2 py-0.5 text-xs text-white">
    {pendingCount}
  </span>
)}
```

---

## 5. Frontend Changes

### 5.1 reportAPI — NEW

**File**: `front-spotLine/src/lib/api.ts` 에 추가

```typescript
export async function createReport(request: {
  targetType: string;
  targetId: string;
  reason: string;
  description?: string;
}): Promise<void> {
  await apiV2.post("/api/v2/reports", request);
}
```

### 5.2 ReportModal — NEW

**File**: `front-spotLine/src/components/comment/ReportModal.tsx`

```typescript
interface ReportModalProps {
  targetType: string;
  targetId: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ReportModal({ targetType, targetId, onClose, onSuccess }: ReportModalProps) {
  const [reason, setReason] = useState<string>("");
  const [description, setDescription] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reasons = [
    { value: "SPAM", label: "스팸" },
    { value: "INAPPROPRIATE", label: "부적절한 내용" },
    { value: "HARASSMENT", label: "괴롭힘/욕설" },
    { value: "OTHER", label: "기타" },
  ];

  const handleSubmit = async () => {
    if (!reason) return;
    setIsSubmitting(true);
    try {
      await createReport({ targetType, targetId, reason, description: description || undefined });
      onSuccess();
    } catch (err: any) {
      if (err?.response?.status === 409) {
        setError("이미 신고한 콘텐츠입니다.");
      } else {
        setError("신고에 실패했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // 모달 UI: 배경 오버레이 + 사유 라디오 버튼 + 상세 textarea + 제출/취소
}
```

### 5.3 CommentItem — 신고 버튼 추가

**File**: `front-spotLine/src/components/comment/CommentItem.tsx`

기존 `CommentMenu` 옆에 비작성자용 신고 버튼 추가:

```tsx
// isOwner가 아닌 경우, 로그인한 사용자에게만 신고 버튼 표시
{!isOwner && currentUserId && !comment.isDeleted && (
  <button
    onClick={() => setShowReportModal(true)}
    className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-500"
    title="신고"
  >
    <Flag size={14} />
  </button>
)}

{showReportModal && (
  <ReportModal
    targetType="COMMENT"
    targetId={comment.id}
    onClose={() => setShowReportModal(false)}
    onSuccess={() => {
      setShowReportModal(false);
      // 성공 토스트 or 알림
    }}
  />
)}
```

---

## 6. Implementation Order (체크리스트)

### Backend (springboot-spotLine-backend)

- [ ] 1. `ReportReason` + `ReportStatus` + `ModerationAction` enum 생성
- [ ] 2. `ContentReport` 엔티티 생성
- [ ] 3. `ContentReportRepository` 생성
- [ ] 4. `CreateReportRequest` + `ResolveReportRequest` + `ReportResponse` DTO 생성
- [ ] 5. `ContentReportService` 생성
- [ ] 6. `ContentReportController` 생성

### Admin (admin-spotLine)

- [ ] 7. `reportAPI.ts` — API 서비스 생성
- [ ] 8. `ModerationQueue.tsx` — 모더레이션 큐 페이지 생성
- [ ] 9. `App.tsx` — `/moderation` 라우트 등록
- [ ] 10. `Layout.tsx` — 사이드바에 모더레이션 메뉴 + 미처리 배지 추가

### Frontend (front-spotLine)

- [ ] 11. `api.ts` — `createReport` 함수 추가
- [ ] 12. `ReportModal.tsx` — 신고 모달 컴포넌트 생성
- [ ] 13. `CommentItem.tsx` — 신고 버튼 + ReportModal 연동

---

## 7. Error Handling

### 7.1 Backend Error Codes

| Code | Message | Cause |
|------|---------|-------|
| 401 | "로그인이 필요합니다" | 비인증 사용자 신고 시도 |
| 404 | "댓글을 찾을 수 없습니다" | 없는 댓글 신고 |
| 404 | "Report를 찾을 수 없습니다" | 없는 신고 처리 |
| 409 | "이미 신고한 콘텐츠입니다" | 중복 신고 |
| 400 | "이미 처리된 신고입니다" | 처리 완료된 신고 재처리 |

### 7.2 Frontend Error Handling

- 409 Conflict → "이미 신고한 콘텐츠입니다." 메시지
- 기타 에러 → "신고에 실패했습니다." 메시지
- 성공 → 모달 닫기

---

## 8. Security

- [x] `POST /api/v2/reports` → `authenticated()` (기존 SecurityConfig 규칙)
- [x] `GET/PUT /api/v2/admin/reports/**` → `hasRole("ADMIN")` (기존 SecurityConfig 규칙)
- [x] 중복 신고 방지: DB unique constraint + 서비스 레벨 체크
- [x] 모더레이션 처리 시 Admin ID 기록 (감사 추적)

---

## 9. Coding Convention Reference

| Item | Convention Applied |
|------|-------------------|
| Backend Entity | `@Builder @Getter @Setter`, unique constraint |
| Backend Enum | 별도 파일, PascalCase values |
| Repository | Spring Data JPA, `@Query` 필요 없음 (메서드명 쿼리) |
| Service | `@Transactional(readOnly=true)` default, write에 `@Transactional` |
| Controller | 사용자 API + Admin API 분리 (경로 기반) |
| Admin Page | React Query + useMutation pattern |
| Frontend | `"use client"`, `cn()` 유틸, 한국어 UI 텍스트 |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
