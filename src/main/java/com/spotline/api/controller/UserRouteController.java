package com.spotline.api.controller;

import com.spotline.api.domain.entity.Route;
import com.spotline.api.domain.entity.UserRoute;
import com.spotline.api.domain.repository.RouteRepository;
import com.spotline.api.dto.request.ReplicateRouteRequest;
import com.spotline.api.dto.request.UpdateMyRouteStatusRequest;
import com.spotline.api.dto.response.MyRouteResponse;
import com.spotline.api.dto.response.ReplicateRouteResponse;
import com.spotline.api.dto.response.RoutePreviewResponse;
import com.spotline.api.dto.response.SimplePageResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class UserRouteController {

    private final UserRouteService userRouteService;
    private final AuthUtil authUtil;
    private final RouteRepository routeRepository;

    @PostMapping("/routes/{routeId}/replicate")
    public ReplicateRouteResponse replicate(
            @PathVariable UUID routeId,
            @RequestBody ReplicateRouteRequest request) {
        return userRouteService.replicate(
            authUtil.requireUserId(), routeId, request.getScheduledDate());
    }

    @GetMapping("/users/me/routes")
    public SimplePageResponse<MyRouteResponse> getMyRoutes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        Page<UserRoute> routes = userRouteService.getMyRoutes(
            authUtil.requireUserId(), status, PageRequest.of(page, 20));
        return new SimplePageResponse<>(
            routes.getContent().stream().map(MyRouteResponse::from).toList(),
            routes.hasNext()
        );
    }

    @PatchMapping("/users/me/routes/{myRouteId}")
    public MyRouteResponse updateStatus(
            @PathVariable UUID myRouteId,
            @RequestBody UpdateMyRouteStatusRequest request) {
        return MyRouteResponse.from(
            userRouteService.updateStatus(authUtil.requireUserId(), myRouteId, request.getStatus()));
    }

    @DeleteMapping("/users/me/routes/{myRouteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyRoute(@PathVariable UUID myRouteId) {
        userRouteService.delete(authUtil.requireUserId(), myRouteId);
    }

    @GetMapping("/routes/{routeId}/variations")
    public SimplePageResponse<RoutePreviewResponse> getVariations(
            @PathVariable UUID routeId,
            @RequestParam(defaultValue = "0") int page) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<RoutePreviewResponse> all = route.getVariations().stream()
            .filter(Route::getIsActive)
            .map(RoutePreviewResponse::from)
            .toList();

        int pageSize = 20;
        int start = Math.min(page * pageSize, all.size());
        int end = Math.min(start + pageSize, all.size());
        return new SimplePageResponse<>(all.subList(start, end), end < all.size());
    }
}
