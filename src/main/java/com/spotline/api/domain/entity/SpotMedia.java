package com.spotline.api.domain.entity;

import com.spotline.api.domain.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spot_media_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpotMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    private String thumbnailS3Key;

    private Integer durationSec;

    @Column(nullable = false)
    private Integer displayOrder;

    private Long fileSizeBytes;

    private String mimeType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
