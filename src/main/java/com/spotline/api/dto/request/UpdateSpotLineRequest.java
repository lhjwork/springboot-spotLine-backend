package com.spotline.api.dto.request;

import com.spotline.api.domain.enums.SpotLineTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "SpotLine 수정 요청")
public class UpdateSpotLineRequest {
    private String title;
    private String description;
    private SpotLineTheme theme;
    private String area;
    private List<CreateSpotLineRequest.SpotLineSpotRequest> spots;
}
