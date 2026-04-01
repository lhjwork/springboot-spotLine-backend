package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_scan_logs", indexes = {
        @Index(name = "idx_scan_log_qr_code", columnList = "qr_code_id"),
        @Index(name = "idx_scan_log_scanned_at", columnList = "scannedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id", nullable = false)
    private PartnerQrCode qrCode;

    /** 익명 세션 ID (Front에서 생성한 클라이언트 세션) */
    private String sessionId;

    private String userAgent;
    private String referer;

    @CreationTimestamp
    private LocalDateTime scannedAt;
}
