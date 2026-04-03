package com.spotline.api.controller;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.dto.request.CreatePartnerRequest;
import com.spotline.api.dto.request.CreateQrCodeRequest;
import com.spotline.api.dto.request.UpdatePartnerRequest;
import com.spotline.api.dto.response.PartnerAnalyticsResponse;
import com.spotline.api.dto.response.PartnerQrCodeResponse;
import com.spotline.api.dto.response.PartnerResponse;
import com.spotline.api.service.PartnerService;
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

@RestController
@RequestMapping("/api/v2/admin/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    // ---- Partner CRUD ----

    @PostMapping
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<PartnerResponse>> list(
            @RequestParam(required = false) PartnerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(partnerService.list(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePartnerRequest request) {
        return ResponseEntity.ok(partnerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- QR Code Management ----

    @PostMapping("/{id}/qr-codes")
    public ResponseEntity<PartnerQrCodeResponse> createQrCode(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQrCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.createQrCode(id, request));
    }

    @GetMapping("/{id}/qr-codes")
    public ResponseEntity<List<PartnerQrCodeResponse>> listQrCodes(@PathVariable UUID id) {
        return ResponseEntity.ok(partnerService.listQrCodes(id));
    }

    @PatchMapping("/{id}/qr-codes/{qrCodeId}")
    public ResponseEntity<PartnerQrCodeResponse> updateQrCode(
            @PathVariable UUID id,
            @PathVariable UUID qrCodeId,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(partnerService.updateQrCode(id, qrCodeId, isActive));
    }

    @DeleteMapping("/{id}/qr-codes/{qrCodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQrCode(@PathVariable UUID id, @PathVariable UUID qrCodeId) {
        partnerService.deleteQrCode(id, qrCodeId);
    }

    // ---- Analytics ----

    @GetMapping("/{id}/analytics")
    public ResponseEntity<PartnerAnalyticsResponse> getAnalytics(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(partnerService.getAnalytics(id, period));
    }
}
