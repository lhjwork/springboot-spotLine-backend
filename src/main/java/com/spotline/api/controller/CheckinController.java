package com.spotline.api.controller;

import com.spotline.api.dto.request.CheckinRequest;
import com.spotline.api.dto.response.CheckinListResponse;
import com.spotline.api.dto.response.CheckinResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Checkin", description = "GPS 기반 체크인")
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final AuthUtil authUtil;

    @Operation(summary = "스팟 체크인")
    @PostMapping("/spots/{id}/checkin")
    public CheckinResponse checkin(@PathVariable UUID id, @Valid @RequestBody CheckinRequest request) {
        return checkinService.checkin(authUtil.requireUserId(), id, request);
    }

    @Operation(summary = "스팟의 체크인 목록")
    @GetMapping("/spots/{id}/checkins")
    public Page<CheckinListResponse> getSpotCheckins(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return checkinService.getSpotCheckins(id, pageable);
    }

    @Operation(summary = "내 체크인 히스토리")
    @GetMapping("/me/checkins")
    public Page<CheckinListResponse> getMyCheckins(@PageableDefault(size = 20) Pageable pageable) {
        return checkinService.getUserCheckins(authUtil.requireUserId(), pageable);
    }
}
