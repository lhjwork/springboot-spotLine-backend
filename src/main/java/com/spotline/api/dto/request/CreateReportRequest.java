package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReportRequest {

    @NotNull
    private UUID targetId;

    @NotBlank
    private String targetType;  // "COMMENT"

    @NotNull
    private ReportReason reason;

    private String description;
}
