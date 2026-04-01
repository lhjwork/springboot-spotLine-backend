package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partners", indexes = {
        @Index(name = "idx_partner_spot", columnList = "spot_id", unique = true),
        @Index(name = "idx_partner_status", columnList = "status"),
        @Index(name = "idx_partner_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false, unique = true)
    private Spot spot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerTier tier;

    /** 브랜딩 컬러 (#FF6B35 형식) */
    @Column(length = 7)
    private String brandColor;

    /** 혜택 텍스트 ("QR 스캔 고객 10% 할인") */
    @Column(length = 500)
    private String benefitText;

    @Column(nullable = false)
    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    /** 관리 메모 (Admin 전용) */
    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerQrCode> qrCodes = new ArrayList<>();

    /** 총 스캔 수 (denormalized for list query performance) */
    @Builder.Default
    private Integer totalScans = 0;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
