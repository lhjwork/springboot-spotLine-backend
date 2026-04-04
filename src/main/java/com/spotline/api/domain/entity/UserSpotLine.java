package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_spotlines", indexes = {
    @Index(name = "idx_user_spotlines_user_id", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSpotLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotline_id", nullable = false)
    private SpotLine spotLine;

    private String scheduledDate;

    @Builder.Default
    private String status = "scheduled";

    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
