package com.spotline.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQrCodeRequest {

    @NotBlank(message = "label은 필수입니다")
    private String label;
}
