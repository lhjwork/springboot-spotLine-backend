package com.spotline.api.service;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserSpotLine;
import com.spotline.api.domain.enums.NotificationType;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.domain.repository.UserSpotLineRepository;
import com.spotline.api.dto.response.ReplicateSpotLineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSpotLineService {

    private final UserSpotLineRepository userSpotLineRepository;
    private final SpotLineRepository spotLineRepository;
    private final NotificationService notificationService;

    public ReplicateSpotLineResponse replicate(String userId, UUID spotLineId, String scheduledDate) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UserSpotLine userSpotLine = userSpotLineRepository.save(UserSpotLine.builder()
            .userId(userId)
            .spotLine(spotLine)
            .scheduledDate(scheduledDate)
            .status("scheduled")
            .build());

        spotLine.setReplicationsCount(spotLine.getReplicationsCount() + 1);
        spotLineRepository.save(spotLine);

        try {
            notificationService.create(userId, spotLine.getCreatorId(), NotificationType.FORK,
                "SPOTLINE", spotLineId.toString(), spotLine.getSlug());
        } catch (Exception ignored) {}

        return ReplicateSpotLineResponse.from(userSpotLine, spotLine);
    }

    @Transactional(readOnly = true)
    public Page<UserSpotLine> getMySpotLines(String userId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return userSpotLineRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return userSpotLineRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public UserSpotLine update(String userId, UUID mySpotLineId, String status, String scheduledDate) {
        UserSpotLine ur = userSpotLineRepository.findById(mySpotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (scheduledDate != null) {
            ur.setScheduledDate(scheduledDate);
        }

        if (status != null) {
            ur.setStatus(status);
            if ("completed".equals(status)) {
                ur.setCompletedAt(LocalDateTime.now());
                SpotLine spotLine = ur.getSpotLine();
                spotLine.setCompletionsCount(spotLine.getCompletionsCount() + 1);
                spotLineRepository.save(spotLine);
            }
        }

        return userSpotLineRepository.save(ur);
    }

    public void delete(String userId, UUID mySpotLineId) {
        UserSpotLine ur = userSpotLineRepository.findById(mySpotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        userSpotLineRepository.delete(ur);
    }
}
