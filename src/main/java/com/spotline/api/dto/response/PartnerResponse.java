package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerStatus;
import com.spotline.api.domain.enums.PartnerTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PartnerResponse {
    private UUID id;
    private UUID spotId;
    private String spotSlug;
    private String spotTitle;
    private PartnerStatus status;
    private PartnerTier tier;
    private String brandColor;
    private String benefitText;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String note;
    private Integer totalScans;
    private Integer qrCodeCount;
    private List<PartnerQrCodeResponse> qrCodes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PartnerResponse from(Partner partner) {
        return from(partner, false);
    }

    public static PartnerResponse from(Partner partner, boolean includeQrCodes) {
        List<PartnerQrCodeResponse> qrCodeResponses = null;
        if (includeQrCodes && partner.getQrCodes() != null) {
            qrCodeResponses = partner.getQrCodes().stream()
                    .map(PartnerQrCodeResponse::from)
                    .toList();
        }

        return PartnerResponse.builder()
                .id(partner.getId())
                .spotId(partner.getSpot().getId())
                .spotSlug(partner.getSpot().getSlug())
                .spotTitle(partner.getSpot().getTitle())
                .status(partner.getStatus())
                .tier(partner.getTier())
                .brandColor(partner.getBrandColor())
                .benefitText(partner.getBenefitText())
                .contractStartDate(partner.getContractStartDate())
                .contractEndDate(partner.getContractEndDate())
                .note(partner.getNote())
                .totalScans(partner.getTotalScans())
                .qrCodeCount(partner.getQrCodes() != null ? partner.getQrCodes().size() : 0)
                .qrCodes(qrCodeResponses)
                .isActive(partner.getIsActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
