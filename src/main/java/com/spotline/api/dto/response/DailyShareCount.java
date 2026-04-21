package com.spotline.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyShareCount {
    private LocalDate date;
    private long count;
}
