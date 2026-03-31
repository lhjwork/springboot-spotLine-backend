package com.spotline.api.service;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.UserRoute;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.domain.repository.UserRouteRepository;
import com.spotline.api.dto.response.ReplicateRouteResponse;
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
public class UserRouteService {

    private final UserRouteRepository userRouteRepository;
    private final RouteRepository routeRepository;

    public ReplicateRouteResponse replicate(String userId, UUID routeId, String scheduledDate) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UserRoute userRoute = userRouteRepository.save(UserRoute.builder()
            .userId(userId)
            .route(route)
            .scheduledDate(scheduledDate)
            .status("scheduled")
            .build());

        route.setReplicationsCount(route.getReplicationsCount() + 1);
        routeRepository.save(route);

        return ReplicateRouteResponse.from(userRoute, route);
    }

    @Transactional(readOnly = true)
    public Page<UserRoute> getMyRoutes(String userId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return userRouteRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return userRouteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public UserRoute updateStatus(String userId, UUID myRouteId, String status) {
        UserRoute ur = userRouteRepository.findById(myRouteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ur.setStatus(status);
        if ("completed".equals(status)) {
            ur.setCompletedAt(LocalDateTime.now());
        }
        return userRouteRepository.save(ur);
    }

    public void delete(String userId, UUID myRouteId) {
        UserRoute ur = userRouteRepository.findById(myRouteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        userRouteRepository.delete(ur);
    }
}
