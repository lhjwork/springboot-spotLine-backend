package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.RouteTheme;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routes", indexes = {
        @Index(name = "idx_route_slug", columnList = "slug", unique = true),
        @Index(name = "idx_route_area", columnList = "area"),
        @Index(name = "idx_route_theme", columnList = "theme"),
        @Index(name = "idx_route_active", columnList = "isActive"),
        @Index(name = "idx_route_parent", columnList = "parent_route_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

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
    private RouteTheme theme;

    /** 대표 지역 */
    @Column(nullable = false)
    private String area;

    /** 총 소요시간 (분) */
    @Builder.Default
    private Integer totalDuration = 0;

    /** 총 거리 (미터) */
    @Builder.Default
    private Integer totalDistance = 0;

    // ---- Route Spots (순서 있는 Spot 목록) ----
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("spotOrder ASC")
    @Builder.Default
    private List<RouteSpot> spots = new ArrayList<>();

    // ---- Stats ----
    @Builder.Default
    private Integer likesCount = 0;
    @Builder.Default
    private Integer savesCount = 0;
    @Builder.Default
    private Integer replicationsCount = 0;
    @Builder.Default
    private Integer completionsCount = 0;

    // ---- Creator ----
    @Column(nullable = false)
    private String creatorType;
    private String creatorId;
    private String creatorName;

    /** 변형 원본 Route */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_route_id")
    private Route parentRoute;

    @OneToMany(mappedBy = "parentRoute")
    @Builder.Default
    private List<Route> variations = new ArrayList<>();

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
