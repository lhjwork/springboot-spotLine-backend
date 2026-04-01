package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_qr_codes", indexes = {
        @Index(name = "idx_qr_code_qr_id", columnList = "qrId", unique = true),
        @Index(name = "idx_qr_code_partner", columnList = "partner_id"),
        @Index(name = "idx_qr_code_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /** 고유 QR ID — URL에 사용 (partner-{spotSlug}-{seq}) */
    @Column(nullable = false, unique = true)
    private String qrId;

    /** 라벨 ("정문", "카운터" 등) */
    @Column(nullable = false, length = 100)
    private String label;

    @Builder.Default
    private Boolean isActive = true;

    /** 스캔 수 (denormalized) */
    @Builder.Default
    private Integer scansCount = 0;

    private LocalDateTime lastScannedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
