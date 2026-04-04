package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "파트너 수정 요청")
public class UpdatePartnerRequest {

    private PartnerStatus status;
    private PartnerTier tier;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "brandColor는 #RRGGBB 형식이어야 합니다")
    private String brandColor;

    private String benefitText;
    private LocalDate contractEndDate;
    private String note;
}
