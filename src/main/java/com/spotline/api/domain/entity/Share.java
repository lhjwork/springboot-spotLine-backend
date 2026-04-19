package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.ShareChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shares", indexes = {
        @Index(name = "idx_shares_target", columnList = "targetType,targetId"),
        @Index(name = "idx_shares_referrer", columnList = "referrerId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareChannel channel;

    private String sharerId;

    private String referrerId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
