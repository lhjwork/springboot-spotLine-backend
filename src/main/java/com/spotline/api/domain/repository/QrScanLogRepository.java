package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.QrScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface QrScanLogRepository extends JpaRepository<QrScanLog, UUID> {

    /** 특정 파트너의 기간별 총 스캔 수 */
    @Query("SELECT COUNT(l) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId AND l.scannedAt >= :since")
    long countByPartnerIdSince(@Param("partnerId") UUID partnerId, @Param("since") LocalDateTime since);

    /** 특정 파트너의 기간별 유니크 세션 수 */
    @Query("SELECT COUNT(DISTINCT l.sessionId) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId AND l.scannedAt >= :since AND l.sessionId IS NOT NULL")
    long countUniqueSessionsByPartnerIdSince(@Param("partnerId") UUID partnerId, @Param("since") LocalDateTime since);

    /** 특정 QR 코드의 총 스캔 수 */
    long countByQrCodeId(UUID qrCodeId);

    /** 일별 스캔 수 집계 */
    @Query("SELECT CAST(l.scannedAt AS date) as scanDate, COUNT(l) as scanCount " +
           "FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId " +
           "AND l.scannedAt >= :since GROUP BY CAST(l.scannedAt AS date) " +
           "ORDER BY scanDate ASC")
    List<Object[]> findDailyScansByPartnerId(
        @Param("partnerId") UUID partnerId,
        @Param("since") LocalDateTime since
    );

    /** 마지막 스캔 시각 */
    @Query("SELECT MAX(l.scannedAt) FROM QrScanLog l WHERE l.qrCode.partner.id = :partnerId")
    LocalDateTime findLastScanAtByPartnerId(@Param("partnerId") UUID partnerId);
}
