package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.PartnerQrCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "파트너 QR 코드 응답")
@Data
@Builder
public class PartnerQrCodeResponse {
    private UUID id;
    private String qrId;
    private String label;
    private Boolean isActive;
    private Integer scansCount;
    private LocalDateTime lastScannedAt;
    private LocalDateTime createdAt;

    public static PartnerQrCodeResponse from(PartnerQrCode qrCode) {
        return PartnerQrCodeResponse.builder()
                .id(qrCode.getId())
                .qrId(qrCode.getQrId())
                .label(qrCode.getLabel())
                .isActive(qrCode.getIsActive())
                .scansCount(qrCode.getScansCount())
                .lastScannedAt(qrCode.getLastScannedAt())
                .createdAt(qrCode.getCreatedAt())
                .build();
    }
}
