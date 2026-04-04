package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "콘텐츠 신고 요청")
public class CreateReportRequest {

    @NotNull
    private UUID targetId;

    @NotBlank
    private String targetType;  // "COMMENT"

    @NotNull
    private ReportReason reason;

    private String description;
}
