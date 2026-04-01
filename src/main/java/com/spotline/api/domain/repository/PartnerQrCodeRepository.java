package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.PartnerQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerQrCodeRepository extends JpaRepository<PartnerQrCode, UUID> {

    /** 파트너의 모든 QR 코드 */
    List<PartnerQrCode> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);

    /** QR ID로 조회 (스캔 시 사용) */
    Optional<PartnerQrCode> findByQrIdAndIsActiveTrue(String qrId);

    /** QR ID 존재 여부 (중복 체크) */
    boolean existsByQrId(String qrId);

    /** 파트너의 QR 코드 수 */
    int countByPartnerIdAndIsActiveTrue(UUID partnerId);
}
