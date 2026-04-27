package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.SpotLineTheme;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "collections", indexes = {
        @Index(name = "idx_collection_slug", columnList = "slug", unique = true),
        @Index(name = "idx_collection_area", columnList = "area"),
        @Index(name = "idx_collection_theme", columnList = "theme"),
        @Index(name = "idx_collection_featured", columnList = "isFeatured"),
        @Index(name = "idx_collection_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    private SpotLineTheme theme;

    private String area;

    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private Boolean isPublished = true;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private Long viewsCount = 0L;

    @Builder.Default
    private Integer itemCount = 0;

    private String createdBy;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    @Builder.Default
    private List<CollectionItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean isActive = true;
}
