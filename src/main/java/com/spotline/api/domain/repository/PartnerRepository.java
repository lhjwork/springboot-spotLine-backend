package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    /** 상태별 파트너 목록 (활성만) */
    Page<Partner> findByStatusAndIsActiveTrue(PartnerStatus status, Pageable pageable);

    /** 전체 활성 파트너 목록 */
    Page<Partner> findByIsActiveTrue(Pageable pageable);

    /** ID로 활성 파트너 조회 (QR 코드 포함) */
    @Query("SELECT p FROM Partner p LEFT JOIN FETCH p.qrCodes WHERE p.id = :id AND p.isActive = true")
    Optional<Partner> findByIdWithQrCodes(@Param("id") UUID id);

    /** Spot ID로 활성 파트너 조회 */
    @Query("SELECT p FROM Partner p WHERE p.spot.id = :spotId AND p.isActive = true")
    Optional<Partner> findBySpotIdAndIsActiveTrue(@Param("spotId") UUID spotId);

    /** Spot에 이미 파트너가 있는지 확인 */
    boolean existsBySpotIdAndIsActiveTrue(UUID spotId);
}
