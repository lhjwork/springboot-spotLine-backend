package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.ModerationAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "신고 처리 요청")
public class ResolveReportRequest {

    @NotNull
    private ModerationAction action;  // HIDE_CONTENT or DISMISS

    private String moderatorNote;
}
