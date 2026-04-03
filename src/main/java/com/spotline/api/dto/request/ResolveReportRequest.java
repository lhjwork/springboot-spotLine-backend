package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.ModerationAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveReportRequest {

    @NotNull
    private ModerationAction action;  // HIDE_CONTENT or DISMISS

    private String moderatorNote;
}
