package com.spotline.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "체크인 패턴 응답 (어드민)")
public class CheckinPatternResponse {

    private List<HourlyItem> hourly;
    private List<DailyItem> daily;

    @Data
    @AllArgsConstructor
    public static class HourlyItem {
        private Integer hour;
        private Long count;
    }

    @Data
    @AllArgsConstructor
    public static class DailyItem {
        private String dayOfWeek;
        private Long count;
    }
}
