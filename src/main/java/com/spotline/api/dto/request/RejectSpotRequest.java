package com.spotline.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectSpotRequest {
    @NotBlank(message = "반려 사유를 입력해주세요")
    private String reason;
}
