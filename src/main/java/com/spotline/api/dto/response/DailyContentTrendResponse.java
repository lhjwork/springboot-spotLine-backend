package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyContentTrendResponse {
    private LocalDate date;
    private long spotCount;
    private long routeCount;
}
