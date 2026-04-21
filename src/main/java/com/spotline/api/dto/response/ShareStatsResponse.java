package com.spotline.api.dto.response;

import com.spotline.api.domain.enums.ShareChannel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ShareStatsResponse {
    private long totalShares;
    private Map<ShareChannel, Long> channelBreakdown;
    private List<DailyShareCount> dailyTrend;
}
