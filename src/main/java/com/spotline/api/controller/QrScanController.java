package com.spotline.api.controller;

import com.spotline.api.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "QR", description = "QR 스캔 기록")
@RestController
@RequestMapping("/api/v2/qr")
@RequiredArgsConstructor
public class QrScanController {

    private final PartnerService partnerService;

    @Operation(summary = "QR 스캔 기록")
    @PostMapping("/{qrId}/scan")
    public ResponseEntity<Void> recordScan(
            @PathVariable String qrId,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referer) {
        partnerService.recordScan(qrId, sessionId, userAgent, referer);
        return ResponseEntity.ok().build();
    }
}
