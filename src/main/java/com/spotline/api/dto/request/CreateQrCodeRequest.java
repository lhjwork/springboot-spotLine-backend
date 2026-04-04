package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "QR 코드 생성 요청")
public class CreateQrCodeRequest {

    @NotBlank(message = "label은 필수입니다")
    private String label;
}
