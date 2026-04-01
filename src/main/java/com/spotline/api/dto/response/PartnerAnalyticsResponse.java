package com.spotline.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PartnerAnalyticsResponse {
    private UUID partnerId;
    private String spotTitle;
    private String period;
    private long totalScans;
    private long uniqueVisitors;
    private double conversionRate;
}
