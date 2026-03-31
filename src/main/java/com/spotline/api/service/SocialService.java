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
    private final RouteRepository routeRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteSaveRepository routeSaveRepository;

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

    public SocialToggleResponse toggleRouteLike(String userId, UUID routeId) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<RouteLike> existing = routeLikeRepository.findByUserIdAndRoute(userId, route);
        boolean liked;
        if (existing.isPresent()) {
            routeLikeRepository.delete(existing.get());
            route.setLikesCount(Math.max(0, route.getLikesCount() - 1));
            liked = false;
        } else {
            routeLikeRepository.save(RouteLike.builder().userId(userId).route(route).build());
            route.setLikesCount(route.getLikesCount() + 1);
            liked = true;
        }
        routeRepository.save(route);
        return new SocialToggleResponse(liked, null, route.getLikesCount(), route.getSavesCount());
    }

    public SocialToggleResponse toggleRouteSave(String userId, UUID routeId) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<RouteSave> existing = routeSaveRepository.findByUserIdAndRoute(userId, route);
        boolean saved;
        if (existing.isPresent()) {
            routeSaveRepository.delete(existing.get());
            route.setSavesCount(Math.max(0, route.getSavesCount() - 1));
            saved = false;
        } else {
            routeSaveRepository.save(RouteSave.builder().userId(userId).route(route).build());
            route.setSavesCount(route.getSavesCount() + 1);
            saved = true;
        }
        routeRepository.save(route);
        return new SocialToggleResponse(null, saved, route.getLikesCount(), route.getSavesCount());
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
    public SocialStatusResponse getRouteSocialStatus(String userId, UUID routeId) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            routeLikeRepository.existsByUserIdAndRoute(userId, route),
            routeSaveRepository.existsByUserIdAndRoute(userId, route)
        );
    }
}
