package com.spotline.api.service;

import com.spotline.api.domain.entity.Share;
import com.spotline.api.domain.entity.Spot;
import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.repository.ShareRepository;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.SpotRepository;
import com.spotline.api.dto.request.ShareRequest;
import com.spotline.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    private final ShareRepository shareRepository;
    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;

    @Transactional
    public int trackShare(ShareRequest request, String sharerId) {
        Share share = Share.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .channel(request.getChannel())
                .sharerId(sharerId)
                .referrerId(request.getReferrerId())
                .build();
        shareRepository.save(share);

        switch (request.getTargetType().toUpperCase()) {
            case "SPOT" -> {
                Spot spot = spotRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Spot", request.getTargetId().toString()));
                spot.setSharesCount(spot.getSharesCount() + 1);
                spotRepository.save(spot);
                return spot.getSharesCount();
            }
            case "SPOTLINE" -> {
                SpotLine spotLine = spotLineRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("SpotLine", request.getTargetId().toString()));
                spotLine.setSharesCount(spotLine.getSharesCount() + 1);
                spotLineRepository.save(spotLine);
                return spotLine.getSharesCount();
            }
            default -> log.warn("알 수 없는 공유 대상 타입: {}", request.getTargetType());
        }

        log.info("공유 추적 완료: {} {} via {}", request.getTargetType(), request.getTargetId(), request.getChannel());
        return 0;
    }
}
