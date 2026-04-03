package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Comment;
import com.spotline.api.domain.entity.ContentReport;
import com.spotline.api.domain.enums.ModerationAction;
import com.spotline.api.domain.enums.ReportReason;
import com.spotline.api.domain.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportResponse {

    private UUID id;
    private String reporterUserId;
    private String targetType;
    private UUID targetId;
    private String targetContent;
    private String targetUserName;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private ModerationAction action;
    private String moderatorNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static ReportResponse from(ContentReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterUserId(report.getReporterUserId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .action(report.getAction())
                .moderatorNote(report.getModeratorNote())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    public static ReportResponse from(ContentReport report, Comment comment) {
        ReportResponse response = from(report);
        if (comment != null) {
            response.setTargetContent(comment.getContent());
            response.setTargetUserName(comment.getUserName());
        }
        return response;
    }
}
