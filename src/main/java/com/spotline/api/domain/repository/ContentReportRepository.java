package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.ContentReport;
import com.spotline.api.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

    boolean existsByReporterUserIdAndTargetTypeAndTargetId(
        String reporterUserId, String targetType, UUID targetId);

    Page<ContentReport> findByStatusOrderByCreatedAtDesc(
        ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
