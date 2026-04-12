package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipientId, createdAt DESC"),
    @Index(name = "idx_notif_unread", columnList = "recipientId, isRead")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(length = 30)
    private String targetType;

    private String targetId;

    @Column(length = 255)
    private String targetSlug;

    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
