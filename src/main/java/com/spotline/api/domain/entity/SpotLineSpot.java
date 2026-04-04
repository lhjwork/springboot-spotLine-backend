package com.spotline.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "spotline_spots", indexes = {
        @Index(name = "idx_spotline_spot_spotline", columnList = "spotline_id"),
        @Index(name = "idx_spotline_spot_spot", columnList = "spot_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotLineSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spotline_id", nullable = false)
    private SpotLine spotLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    /** 순서 (1부터 시작) */
    @Column(nullable = false)
    private Integer spotOrder;

    /** 추천 방문 시각 (예: "17:30") */
    private String suggestedTime;

    /** 체류 시간 (분) */
    private Integer stayDuration;

    /** 다음 Spot까지 도보 시간 (분) */
    private Integer walkingTimeToNext;

    /** 다음 Spot까지 거리 (미터) */
    private Integer distanceToNext;

    /** 이동 참고 (예: "골목길로 5분") */
    private String transitionNote;
}
