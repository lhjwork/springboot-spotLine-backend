package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "일별 콘텐츠 추이 응답")
@Data
@Builder
public class DailyContentTrendResponse {
    private LocalDate date;
    private long spotCount;
    private long spotLineCount;
}
