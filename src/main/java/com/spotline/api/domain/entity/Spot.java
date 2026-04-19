package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.SpotCategory;
import com.spotline.api.domain.enums.SpotSource;
import com.spotline.api.domain.enums.SpotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "spots", indexes = {
        @Index(name = "idx_spot_slug", columnList = "slug", unique = true),
        @Index(name = "idx_spot_area", columnList = "area"),
        @Index(name = "idx_spot_category", columnList = "category"),
        @Index(name = "idx_spot_source", columnList = "source"),
        @Index(name = "idx_spot_active", columnList = "isActive"),
        @Index(name = "idx_spot_lat_lng", columnList = "latitude, longitude"),
        @Index(name = "idx_spot_sigungu", columnList = "sigungu"),
        @Index(name = "idx_spot_sido", columnList = "sido"),
        @Index(name = "idx_spot_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotSource source;

    /** 크루 한줄 추천 — 차별화 핵심 */
    @Column(length = 500)
    private String crewNote;

    // ---- Location ----
    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /** 지역 (성수, 을지로, 연남 등) */
    @Column(nullable = false)
    private String area;

    /** 시/도 (서울, 경기 등) */
    private String sido;

    /** 시/군/구 (마포구, 강남구 등) */
    private String sigungu;

    /** 동/읍/면 (연남동, 성수동 등) */
    private String dong;

    // ---- External Links ----
    private String blogUrl;
    private String instagramUrl;
    private String websiteUrl;

    // ---- External Place API IDs ----
    private String naverPlaceId;
    private String kakaoPlaceId;

    // ---- Tags ----
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "spot_tags", joinColumns = @JoinColumn(name = "spot_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // ---- Media (S3 keys) — legacy, 호환용 유지 ----
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "spot_media", joinColumns = @JoinColumn(name = "spot_id"))
    @Column(name = "media_key")
    @Builder.Default
    private List<String> media = new ArrayList<>();

    // ---- Media Items (structured) ----
    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<SpotMedia> mediaItems = new ArrayList<>();

    // ---- QR (향후 파트너 매장용) ----
    private String qrId;
    @Builder.Default
    private Boolean qrActive = false;

    // ---- Stats ----
    @Builder.Default
    private Integer likesCount = 0;
    @Builder.Default
    private Integer savesCount = 0;
    @Builder.Default
    private Integer viewsCount = 0;
    @Builder.Default
    private Integer commentsCount = 0;
    @Builder.Default
    private Integer visitedCount = 0;
    @Builder.Default
    private Integer sharesCount = 0;

    // ---- Creator ----
    @Column(nullable = false)
    private String creatorType; // "crew" or "user"
    private String creatorId;
    private String creatorName;

    // ---- Approval Workflow ----
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SpotStatus status = SpotStatus.APPROVED;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
