package com.spotline.api.controller;

import com.spotline.api.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/qr")
@RequiredArgsConstructor
public class QrScanController {

    private final PartnerService partnerService;

    /** QR 스캔 로그 기록 — fire-and-forget, 항상 200 반환 */
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
