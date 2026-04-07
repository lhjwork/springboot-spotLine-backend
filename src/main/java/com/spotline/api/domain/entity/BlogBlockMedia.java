package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "blog_block_media", indexes = {
        @Index(name = "idx_media_block", columnList = "blog_block_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogBlockMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_block_id", nullable = false)
    private BlogBlock blogBlock;

    @Column(nullable = false)
    private String mediaUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer mediaOrder = 0;

    private String caption;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
