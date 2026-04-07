package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blog_slug", columnList = "slug", unique = true),
        @Index(name = "idx_blog_user", columnList = "userId"),
        @Index(name = "idx_blog_spotline", columnList = "spotline_id"),
        @Index(name = "idx_blog_status", columnList = "status"),
        @Index(name = "idx_blog_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotline_id", nullable = false)
    private SpotLine spotLine;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userName;

    private String userAvatarUrl;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BlogStatus status = BlogStatus.DRAFT;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("blockOrder ASC")
    @Builder.Default
    private List<BlogBlock> blocks = new ArrayList<>();

    // ---- Stats ----
    @Builder.Default
    private Integer viewsCount = 0;
    @Builder.Default
    private Integer likesCount = 0;
    @Builder.Default
    private Integer savesCount = 0;
    @Builder.Default
    private Integer commentsCount = 0;

    private LocalDateTime publishedAt;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
