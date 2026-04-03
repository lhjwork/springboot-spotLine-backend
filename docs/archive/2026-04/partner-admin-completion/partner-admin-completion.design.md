# Partner Admin Completion Design Document

> **Summary**: 파트너 관리 미완성 기능 보완 — Admin Edit 페이지, Backend dailyTrend/lastScanAt, QR 삭제 API
>
> **Project**: Spotline Admin + Backend
> **Author**: AI Assistant
> **Date**: 2026-04-03
> **Status**: Draft
> **Planning Doc**: [partner-admin-completion.plan.md](../01-plan/features/partner-admin-completion.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- 기존 파트너 CRUD 패턴과 완전 일관성 유지
- Admin PartnerForm 재사용으로 코드 중복 최소화
- Backend PartnerAnalyticsResponse에 dailyTrend + lastScanAt 추가
- QR 코드 삭제 엔드포인트 추가 (기존 deactivate 패턴 활용)

### 1.2 Design Principles

- Backend: 기존 PartnerService/Controller 패턴 확장
- Admin: 기존 React Query + PartnerForm 재사용
- 변경 최소화: 기존 코드 수정 < 신규 코드 생성

---

## 2. Architecture

### 2.1 변경 Data Flow

```
[파트너 수정 — Admin Edit Page]
/partners/:id/edit 접근
  → partnerAPI.getById(id) → 기존 데이터 fetch
  → PartnerForm(mode="edit", initialData=partner)
  → onSubmit → partnerAPI.update(id, data) → PATCH /api/v2/admin/partners/{id}
  → 성공 → navigate(/partners/:id)

[Analytics dailyTrend — Backend 보완]
GET /api/v2/admin/partners/{id}/analytics?period=30d
  → PartnerService.getAnalytics()
  → QrScanLogRepository.findDailyScansByPartnerId(partnerId, since)
  → GROUP BY DATE(scannedAt) → List<DailyScanTrend>
  → 빈 날짜 채우기 (0 scans)
  → PartnerAnalyticsResponse { ..., dailyTrend, lastScanAt }

[QR 코드 삭제 — Soft Delete]
DELETE /api/v2/admin/partners/{id}/qr-codes/{qrCodeId}
  → PartnerService.deleteQrCode(partnerId, qrCodeId)
  → isActive = false (기존 deactivate 로직 재사용)
  → 204 No Content
```

---

## 3. Backend Changes

### 3.1 PartnerAnalyticsResponse — 필드 추가

**File**: `dto/response/PartnerAnalyticsResponse.java`

```java
@Data
@Builder
public class PartnerAnalyticsResponse {
    private UUID partnerId;
    private String spotTitle;
    private String period;
    private long totalScans;
    private long uniqueVisitors;
    private double conversionRate;
    private LocalDateTime lastScanAt;                    // NEW
    private List<DailyScanTrend> dailyTrend;             // NEW

    @Data
    @AllArgsConstructor
    public static class DailyScanTrend {
        private String date;   // "2026-04-01"
        private long scans;
    }
}
```

### 3.2 QrScanLogRepository — dailyTrend 쿼리 추가

**File**: `domain/repository/QrScanLogRepository.java`

```java
/** 일별 스캔 수 집계 */
@Query("SELECT CAST(l.scannedAt AS date) as scanDate, COUNT(l) as scanCount " +
       "FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId " +
       "AND l.scannedAt >= :since GROUP BY CAST(l.scannedAt AS date) " +
       "ORDER BY scanDate ASC")
List<Object[]> findDailyScansByPartnerId(
    @Param("partnerId") UUID partnerId,
    @Param("since") LocalDateTime since
);

/** 마지막 스캔 시각 */
@Query("SELECT MAX(l.scannedAt) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId")
LocalDateTime findLastScanAtByPartnerId(@Param("partnerId") UUID partnerId);
```

### 3.3 PartnerService.getAnalytics() — dailyTrend 로직 추가

**File**: `service/PartnerService.java` — `getAnalytics` 메서드 수정

```java
public PartnerAnalyticsResponse getAnalytics(UUID partnerId, String period) {
    Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException("Partner", partnerId.toString()));

    LocalDateTime since = parsePeriod(period);
    long totalScans = scanLogRepository.countByPartnerIdSince(partnerId, since);
    long uniqueVisitors = scanLogRepository.countUniqueSessionsByPartnerIdSince(partnerId, since);
    double conversionRate = totalScans > 0 ? (double) uniqueVisitors / totalScans : 0;

    // NEW: dailyTrend
    List<Object[]> rawDaily = scanLogRepository.findDailyScansByPartnerId(partnerId, since);
    List<PartnerAnalyticsResponse.DailyScanTrend> dailyTrend = buildDailyTrend(since, rawDaily);

    // NEW: lastScanAt
    LocalDateTime lastScanAt = scanLogRepository.findLastScanAtByPartnerId(partnerId);

    return PartnerAnalyticsResponse.builder()
            .partnerId(partnerId)
            .spotTitle(partner.getSpot().getTitle())
            .period(period != null ? period : "30d")
            .totalScans(totalScans)
            .uniqueVisitors(uniqueVisitors)
            .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
            .lastScanAt(lastScanAt)
            .dailyTrend(dailyTrend)
            .build();
}

/** 빈 날짜를 0으로 채워 연속 일별 데이터 생성 */
private List<PartnerAnalyticsResponse.DailyScanTrend> buildDailyTrend(
        LocalDateTime since, List<Object[]> rawDaily) {
    Map<String, Long> dateMap = new LinkedHashMap<>();
    for (Object[] row : rawDaily) {
        String date = row[0].toString().substring(0, 10); // "YYYY-MM-DD"
        long count = ((Number) row[1]).longValue();
        dateMap.put(date, count);
    }

    List<PartnerAnalyticsResponse.DailyScanTrend> result = new ArrayList<>();
    LocalDate start = since.toLocalDate();
    LocalDate end = LocalDate.now();
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
        String dateStr = d.toString();
        result.add(new PartnerAnalyticsResponse.DailyScanTrend(
                dateStr, dateMap.getOrDefault(dateStr, 0L)));
    }
    return result;
}
```

### 3.4 PartnerController — QR 삭제 엔드포인트 추가

**File**: `controller/PartnerController.java`

기존 `// ---- QR Code Management ----` 섹션에 추가:

```java
@DeleteMapping("/{id}/qr-codes/{qrCodeId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteQrCode(@PathVariable UUID id, @PathVariable UUID qrCodeId) {
    partnerService.deleteQrCode(id, qrCodeId);
}
```

### 3.5 PartnerService — deleteQrCode 메서드 추가

**File**: `service/PartnerService.java`

기존 `updateQrCode` 메서드 아래에 추가:

```java
/** QR 코드 삭제 (soft delete) */
@Transactional
public void deleteQrCode(UUID partnerId, UUID qrCodeId) {
    PartnerQrCode qrCode = qrCodeRepository.findById(qrCodeId)
            .orElseThrow(() -> new ResourceNotFoundException("QrCode", qrCodeId.toString()));

    if (!qrCode.getPartner().getId().equals(partnerId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 파트너의 QR 코드가 아닙니다");
    }

    qrCode.setIsActive(false);
}
```

---

## 4. Admin Changes

### 4.1 PartnerForm — mode + initialData props 추가

**File**: `admin-spotLine/src/components/PartnerForm.tsx`

```typescript
interface PartnerFormProps {
  mode?: "create" | "edit";                    // NEW (default: "create")
  initialData?: PartnerDetailResponse;         // NEW (edit 시 기존 데이터)
  onSubmit: (data: CreatePartnerRequest | UpdatePartnerRequest) => void;
  isSubmitting: boolean;
  error: string | null;
}
```

**변경 사항:**
- `mode` prop 추가 (기본값 `"create"`)
- `initialData` prop 추가 — edit 모드일 때 기존 데이터로 state 초기화
- edit 모드: Spot 선택 섹션을 읽기 전용으로 표시 (변경 불가)
- edit 모드: contractStartDate 표시하되 수정 불가
- submit 버튼 텍스트: "파트너 등록" → "수정 저장" (edit 모드)
- `handleSubmit`에서 edit 모드일 때 `UpdatePartnerRequest`만 전송 (spotId, contractStartDate 제외)

### 4.2 PartnerEdit 페이지 — NEW

**File**: `admin-spotLine/src/pages/PartnerEdit.tsx`

```typescript
export default function PartnerEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: partner, isLoading } = useQuery({
    queryKey: ["partner", id],
    queryFn: () => partnerAPI.getById(id!),
    select: (res) => res.data,
    enabled: !!id,
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdatePartnerRequest) => partnerAPI.update(id!, data),
    onSuccess: () => navigate(`/partners/${id}`),
  });

  if (isLoading) return <LoadingSkeleton />;
  if (!partner) return <NotFoundMessage />;

  return (
    <div className="mx-auto max-w-2xl">
      <BackButton to={`/partners/${id}`} label={partner.spotTitle} />
      <h1 className="mb-6 text-2xl font-bold text-gray-900">파트너 정보 수정</h1>
      <PartnerForm
        mode="edit"
        initialData={partner}
        onSubmit={(data) => updateMutation.mutate(data as UpdatePartnerRequest)}
        isSubmitting={updateMutation.isPending}
        error={updateMutation.isError ? "수정에 실패했습니다." : null}
      />
    </div>
  );
}
```

### 4.3 App.tsx — 라우트 등록

**File**: `admin-spotLine/src/App.tsx`

기존 `partners/:id` 라우트 아래에 추가:

```tsx
<Route path="partners/:id/edit" element={
  <ProtectedRoute requiredRole="admin"><PartnerEdit /></ProtectedRoute>
} />
```

**주의**: `partners/:id/edit`를 `partners/:id` 보다 **위에** 배치하거나, 동일 레벨에 배치 (React Router v6는 specificity 기반이므로 순서 무관하지만, 가독성을 위해 `partners/new` 아래, `partners/:id` 위에 배치).

### 4.4 partnerAPI — deleteQRCode 함수 추가

**File**: `admin-spotLine/src/services/v2/partnerAPI.ts`

```typescript
deleteQRCode: (partnerId: string, qrCodeId: string) =>
  apiClient.delete(`/api/v2/admin/partners/${partnerId}/qr-codes/${qrCodeId}`),
```

### 4.5 QRCodePreview — 삭제 버튼 추가

**File**: `admin-spotLine/src/components/QRCodePreview.tsx`

```typescript
interface QRCodePreviewProps {
  qrCode: PartnerQRCodeResponse;
  onDeactivate?: (qrCodeId: string) => void;
  onDelete?: (qrCodeId: string) => void;        // NEW
}
```

비활성화 버튼 옆에 삭제 버튼 추가 (confirm 포함):

```tsx
{!qrCode.isActive && onDelete && (
  <button
    onClick={() => {
      if (window.confirm("QR 코드를 삭제하시겠습니까?")) {
        onDelete(qrCode.id);
      }
    }}
    className="rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50"
  >
    삭제
  </button>
)}
```

**UX 결정**: 삭제 버튼은 비활성 상태인 QR 코드에만 표시 (활성 상태에서는 먼저 비활성화 필요).

### 4.6 QRCodeManager — 삭제 핸들러 추가

**File**: `admin-spotLine/src/components/QRCodeManager.tsx`

```typescript
const deleteMutation = useMutation({
  mutationFn: (qrCodeId: string) => partnerAPI.deleteQRCode(partnerId, qrCodeId),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ["partner", partnerId] });
  },
});
```

QRCodePreview에 `onDelete` prop 전달:

```tsx
<QRCodePreview
  key={qr.id}
  qrCode={qr}
  onDeactivate={(id) => deactivateMutation.mutate(id)}
  onDelete={(id) => deleteMutation.mutate(id)}
/>
```

---

## 5. Implementation Order (체크리스트)

### Backend (springboot-spotLine-backend)

- [ ] 1. `PartnerAnalyticsResponse` — `lastScanAt` 필드 + `dailyTrend` 필드 + `DailyScanTrend` inner class 추가
- [ ] 2. `QrScanLogRepository` — `findDailyScansByPartnerId` + `findLastScanAtByPartnerId` @Query 추가
- [ ] 3. `PartnerService.getAnalytics()` — dailyTrend 조회 + buildDailyTrend 헬퍼 + lastScanAt 조회
- [ ] 4. `PartnerService.deleteQrCode()` — QR 코드 soft delete 메서드 추가
- [ ] 5. `PartnerController` — `DELETE /{id}/qr-codes/{qrCodeId}` 엔드포인트 추가

### Admin (admin-spotLine)

- [ ] 6. `partnerAPI.ts` — `deleteQRCode` 함수 추가
- [ ] 7. `PartnerForm.tsx` — `mode` + `initialData` props 추가, create/edit 분기 로직
- [ ] 8. `PartnerEdit.tsx` — NEW 페이지 (기존 데이터 fetch + PartnerForm edit 모드)
- [ ] 9. `App.tsx` — `/partners/:id/edit` 라우트 등록 + import
- [ ] 10. `QRCodePreview.tsx` — `onDelete` prop + 삭제 버튼 추가
- [ ] 11. `QRCodeManager.tsx` — deleteMutation + `onDelete` prop 전달

---

## 6. Error Handling

### 6.1 Backend Error Codes

| Code | Message | Cause |
|------|---------|-------|
| 403 | "해당 파트너의 QR 코드가 아닙니다" | QR 삭제 시 partnerId 불일치 |
| 404 | "QrCode를 찾을 수 없습니다" | 없는 qrCodeId |
| 404 | "Partner를 찾을 수 없습니다" | 없는 partnerId |

### 6.2 Frontend Error Handling

- API 실패 시 에러 메시지 표시 (기존 패턴 따름)
- Edit form 에러: "수정에 실패했습니다." 메시지
- QR 삭제 실패: 기존 deactivate 에러 패턴 동일

---

## 7. Security

- [x] 모든 엔드포인트: `/api/v2/admin/**` → `hasRole("ADMIN")` (기존 SecurityConfig)
- [x] QR 삭제 시 partnerId 소유권 검증
- [x] Admin Edit 페이지: `<ProtectedRoute requiredRole="admin">` 보호

---

## 8. Coding Convention Reference

| Item | Convention Applied |
|------|-------------------|
| Backend DTO | `@Data @Builder`, inner class for DailyScanTrend |
| Repository | `@Query` JPQL for complex queries |
| Service | `@Transactional` for writes, readOnly for reads |
| Controller | `@ResponseStatus(HttpStatus.NO_CONTENT)` for DELETE |
| Admin Page | React Query + useMutation pattern |
| Admin Form | Controlled inputs + `mode` prop 분기 |
| Admin Route | `<ProtectedRoute requiredRole="admin">` |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-04-03 | Initial draft | AI Assistant |
