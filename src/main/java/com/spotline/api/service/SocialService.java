package com.spotline.api.service;

import com.spotline.api.domain.entity.*;
import com.spotline.api.domain.repository.*;
import com.spotline.api.dto.response.SocialStatusResponse;
import com.spotline.api.dto.response.SocialToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {

    private final SpotRepository spotRepository;
    private final SpotLineRepository spotLineRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final SpotLineLikeRepository spotLineLikeRepository;
    private final SpotLineSaveRepository spotLineSaveRepository;

    public SocialToggleResponse toggleSpotLike(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLike> existing = spotLikeRepository.findByUserIdAndSpot(userId, spot);
        boolean liked;
        if (existing.isPresent()) {
            spotLikeRepository.delete(existing.get());
            spot.setLikesCount(Math.max(0, spot.getLikesCount() - 1));
            liked = false;
        } else {
            spotLikeRepository.save(SpotLike.builder().userId(userId).spot(spot).build());
            spot.setLikesCount(spot.getLikesCount() + 1);
            liked = true;
        }
        spotRepository.save(spot);
        return new SocialToggleResponse(liked, null, spot.getLikesCount(), spot.getSavesCount());
    }

    public SocialToggleResponse toggleSpotSave(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotSave> existing = spotSaveRepository.findByUserIdAndSpot(userId, spot);
        boolean saved;
        if (existing.isPresent()) {
            spotSaveRepository.delete(existing.get());
            spot.setSavesCount(Math.max(0, spot.getSavesCount() - 1));
            saved = false;
        } else {
            spotSaveRepository.save(SpotSave.builder().userId(userId).spot(spot).build());
            spot.setSavesCount(spot.getSavesCount() + 1);
            saved = true;
        }
        spotRepository.save(spot);
        return new SocialToggleResponse(null, saved, spot.getLikesCount(), spot.getSavesCount());
    }

    public SocialToggleResponse toggleSpotLineLike(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLineLike> existing = spotLineLikeRepository.findByUserIdAndSpotLine(userId, spotLine);
        boolean liked;
        if (existing.isPresent()) {
            spotLineLikeRepository.delete(existing.get());
            spotLine.setLikesCount(Math.max(0, spotLine.getLikesCount() - 1));
            liked = false;
        } else {
            spotLineLikeRepository.save(SpotLineLike.builder().userId(userId).spotLine(spotLine).build());
            spotLine.setLikesCount(spotLine.getLikesCount() + 1);
            liked = true;
        }
        spotLineRepository.save(spotLine);
        return new SocialToggleResponse(liked, null, spotLine.getLikesCount(), spotLine.getSavesCount());
    }

    public SocialToggleResponse toggleSpotLineSave(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLineSave> existing = spotLineSaveRepository.findByUserIdAndSpotLine(userId, spotLine);
        boolean saved;
        if (existing.isPresent()) {
            spotLineSaveRepository.delete(existing.get());
            spotLine.setSavesCount(Math.max(0, spotLine.getSavesCount() - 1));
            saved = false;
        } else {
            spotLineSaveRepository.save(SpotLineSave.builder().userId(userId).spotLine(spotLine).build());
            spotLine.setSavesCount(spotLine.getSavesCount() + 1);
            saved = true;
        }
        spotLineRepository.save(spotLine);
        return new SocialToggleResponse(null, saved, spotLine.getLikesCount(), spotLine.getSavesCount());
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse getSpotSocialStatus(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            spotLikeRepository.existsByUserIdAndSpot(userId, spot),
            spotSaveRepository.existsByUserIdAndSpot(userId, spot)
        );
    }

    @Transactional(readOnly = true)
    public SocialStatusResponse getSpotLineSocialStatus(String userId, UUID spotLineId) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            spotLineLikeRepository.existsByUserIdAndSpotLine(userId, spotLine),
            spotLineSaveRepository.existsByUserIdAndSpotLine(userId, spotLine)
        );
    }
}
