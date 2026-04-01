package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.PartnerTier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreatePartnerRequest {

    @NotNull(message = "spotId는 필수입니다")
    private UUID spotId;

    @NotNull(message = "tier는 필수입니다")
    private PartnerTier tier;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "brandColor는 #RRGGBB 형식이어야 합니다")
    private String brandColor;

    private String benefitText;

    @NotNull(message = "contractStartDate는 필수입니다")
    private LocalDate contractStartDate;

    private LocalDate contractEndDate;
    private String note;
}
