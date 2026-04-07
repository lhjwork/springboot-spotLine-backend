package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.BlogBlockType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blog_blocks", indexes = {
        @Index(name = "idx_block_blog", columnList = "blog_id"),
        @Index(name = "idx_block_order", columnList = "blog_id, blockOrder")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogBlockType blockType;

    @Column(nullable = false)
    private Integer blockOrder;

    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "blogBlock", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("mediaOrder ASC")
    @Builder.Default
    private List<BlogBlockMedia> mediaItems = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
