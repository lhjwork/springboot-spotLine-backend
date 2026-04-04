package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Partner;
import com.spotline.api.domain.enums.PartnerTier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "스팟 파트너 정보")
@Data
@Builder
public class SpotPartnerInfo {
    private boolean isPartner;
    private String brandColor;
    private String benefitText;
    private PartnerTier tier;
    private LocalDate partnerSince;

    public static SpotPartnerInfo from(Partner partner) {
        return SpotPartnerInfo.builder()
                .isPartner(true)
                .brandColor(partner.getBrandColor())
                .benefitText(partner.getBenefitText())
                .tier(partner.getTier())
                .partnerSince(partner.getContractStartDate())
                .build();
    }
}
