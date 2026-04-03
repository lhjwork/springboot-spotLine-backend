package com.spotline.api.service;

import com.spotline.api.domain.entity.Comment;
import com.spotline.api.domain.entity.ContentReport;
import com.spotline.api.domain.enums.ModerationAction;
import com.spotline.api.domain.enums.ReportStatus;
import com.spotline.api.domain.repository.CommentRepository;
import com.spotline.api.domain.repository.ContentReportRepository;
import com.spotline.api.dto.request.CreateReportRequest;
import com.spotline.api.dto.request.ResolveReportRequest;
import com.spotline.api.dto.response.ReportResponse;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentReportService {

    private final ContentReportRepository reportRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse create(String userId, CreateReportRequest request) {
        if (reportRepository.existsByReporterUserIdAndTargetTypeAndTargetId(
                userId, request.getTargetType(), request.getTargetId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 콘텐츠입니다");
        }

        if ("COMMENT".equals(request.getTargetType())) {
            commentRepository.findById(request.getTargetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        }

        ContentReport report = ContentReport.builder()
                .reporterUserId(userId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }

    public Page<ReportResponse> list(ReportStatus status, Pageable pageable) {
        Page<ContentReport> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status != null ? status : ReportStatus.PENDING, pageable);

        return reports.map(report -> {
            if ("COMMENT".equals(report.getTargetType())) {
                Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                return ReportResponse.from(report, comment);
            }
            return ReportResponse.from(report);
        });
    }

    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Transactional
    public ReportResponse resolve(UUID reportId, String adminId, ResolveReportRequest request) {
        ContentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId.toString()));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 신고입니다");
        }

        if (request.getAction() == ModerationAction.HIDE_CONTENT
                && "COMMENT".equals(report.getTargetType())) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
                comment.setIsDeleted(true);
                commentRepository.save(comment);
            });
        }

        report.setStatus(request.getAction() == ModerationAction.DISMISS
                ? ReportStatus.DISMISSED : ReportStatus.RESOLVED);
        report.setAction(request.getAction());
        report.setResolvedByAdminId(adminId);
        report.setResolvedAt(LocalDateTime.now());
        report.setModeratorNote(request.getModeratorNote());

        return ReportResponse.from(reportRepository.save(report));
    }
}
