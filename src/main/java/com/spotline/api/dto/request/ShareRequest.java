package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.ShareChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "공유 추적 요청")
public class ShareRequest {

    @NotBlank(message = "대상 타입은 필수입니다")
    @Schema(description = "공유 대상 타입", example = "SPOT")
    private String targetType;

    @NotNull(message = "대상 ID는 필수입니다")
    @Schema(description = "공유 대상 ID")
    private UUID targetId;

    @NotNull(message = "공유 채널은 필수입니다")
    @Schema(description = "공유 채널")
    private ShareChannel channel;

    @Schema(description = "레퍼럴 사용자 ID (공유 링크를 통해 들어온 경우)")
    private String referrerId;
}
