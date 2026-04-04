package com.spotline.api.controller;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.dto.request.CreatePartnerRequest;
import com.spotline.api.dto.request.CreateQrCodeRequest;
import com.spotline.api.dto.request.UpdatePartnerRequest;
import com.spotline.api.dto.response.PartnerAnalyticsResponse;
import com.spotline.api.dto.response.PartnerQrCodeResponse;
import com.spotline.api.dto.response.PartnerResponse;
import com.spotline.api.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Partner", description = "QR 파트너 매장 관리 (관리자)")
@RestController
@RequestMapping("/api/v2/admin/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "파트너 등록")
    @PostMapping
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.create(request));
    }

    @Operation(summary = "파트너 목록")
    @GetMapping
    public ResponseEntity<Page<PartnerResponse>> list(
            @RequestParam(required = false) PartnerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(partnerService.list(status, pageable));
    }

    @Operation(summary = "파트너 상세")
    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.getById(id));
    }

    @Operation(summary = "파트너 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePartnerRequest request) {
        return ResponseEntity.ok(partnerService.update(id, request));
    }

    @Operation(summary = "파트너 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "QR 코드 생성")
    @PostMapping("/{id}/qr-codes")
    public ResponseEntity<PartnerQrCodeResponse> createQrCode(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQrCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.createQrCode(id, request));
    }

    @Operation(summary = "QR 코드 목록")
    @GetMapping("/{id}/qr-codes")
    public ResponseEntity<List<PartnerQrCodeResponse>> listQrCodes(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.listQrCodes(id));
    }

    @Operation(summary = "QR 코드 상태 변경")
    @PatchMapping("/{id}/qr-codes/{qrCodeId}")
    public ResponseEntity<PartnerQrCodeResponse> updateQrCode(
            @PathVariable UUID id,
            @PathVariable UUID qrCodeId,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(partnerService.updateQrCode(id, qrCodeId, isActive));
    }

    @Operation(summary = "QR 코드 삭제")
    @DeleteMapping("/{id}/qr-codes/{qrCodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQrCode(@PathVariable UUID id, @PathVariable UUID qrCodeId) {
        partnerService.deleteQrCode(id, qrCodeId);
    }

    @Operation(summary = "파트너 분석")
    @GetMapping("/{id}/analytics")
    public ResponseEntity<PartnerAnalyticsResponse> getAnalytics(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(partnerService.getAnalytics(id, period));
    }
}
