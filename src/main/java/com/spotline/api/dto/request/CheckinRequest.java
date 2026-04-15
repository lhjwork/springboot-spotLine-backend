package com.spotline.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "체크인 요청")
public class CheckinRequest {

    @Schema(description = "유저 위도 (GPS 불가 시 null)", example = "37.5445")
    private Double latitude;

    @Schema(description = "유저 경도 (GPS 불가 시 null)", example = "127.0567")
    private Double longitude;

    @Size(max = 100, message = "메모는 100자 이내로 작성해주세요")
    @Schema(description = "체크인 메모 (선택, 100자)", example = "분위기 좋은 카페")
    private String memo;
}
