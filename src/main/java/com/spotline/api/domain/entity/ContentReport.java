package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.ModerationAction;
import com.spotline.api.domain.enums.ReportReason;
import com.spotline.api.domain.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_reports", indexes = {
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_target", columnList = "targetType, targetId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_report_user_target",
        columnNames = {"reporterUserId", "targetType", "targetId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String reporterUserId;

    @Column(nullable = false)
    private String targetType;  // "COMMENT" (향후 "SPOT", "ROUTE" 확장)

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
