package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_items", indexes = {
        @Index(name = "idx_ci_collection_id", columnList = "collection_id"),
        @Index(name = "idx_ci_spot_id", columnList = "spot_id"),
        @Index(name = "idx_ci_spotline_id", columnList = "spot_line_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_line_id")
    private SpotLine spotLine;

    @Column(nullable = false)
    private Integer itemOrder;

    private String itemNote;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean isActive = true;
}
