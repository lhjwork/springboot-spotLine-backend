package com.spotline.api.controller;

import com.spotline.api.domain.entity.SpotLine;
import com.spotline.api.domain.entity.UserSpotLine;
import com.spotline.api.domain.repository.SpotLineRepository;
import com.spotline.api.dto.request.ReplicateSpotLineRequest;
import com.spotline.api.dto.request.UpdateMySpotLineStatusRequest;
import com.spotline.api.dto.response.MySpotLineResponse;
import com.spotline.api.dto.response.ReplicateSpotLineResponse;
import com.spotline.api.dto.response.SpotLinePreviewResponse;
import com.spotline.api.dto.response.SimplePageResponse;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.UserSpotLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Tag(name = "UserSpotLine", description = "SpotLine 복제 + 내 SpotLine 관리")
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class UserSpotLineController {

    private final UserSpotLineService userSpotLineService;
    private final AuthUtil authUtil;
    private final SpotLineRepository spotLineRepository;

    @Operation(summary = "SpotLine 복제 (내 일정으로)")
    @PostMapping("/spotlines/{spotLineId}/replicate")
    public ReplicateSpotLineResponse replicate(
            @PathVariable UUID spotLineId,
            @RequestBody ReplicateSpotLineRequest request) {
        return userSpotLineService.replicate(
            authUtil.requireUserId(), spotLineId, request.getScheduledDate());
    }

    @Operation(summary = "내 SpotLine 목록")
    @GetMapping("/users/me/spotlines")
    public SimplePageResponse<MySpotLineResponse> getMySpotLines(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        Page<UserSpotLine> spotLines = userSpotLineService.getMySpotLines(
            authUtil.requireUserId(), status, PageRequest.of(page, 20));
        return new SimplePageResponse<>(
            spotLines.getContent().stream().map(MySpotLineResponse::from).toList(),
            spotLines.hasNext()
        );
    }

    @Operation(summary = "내 SpotLine 업데이트")
    @PatchMapping("/users/me/spotlines/{mySpotLineId}")
    public MySpotLineResponse updateMySpotLine(
            @PathVariable UUID mySpotLineId,
            @RequestBody UpdateMySpotLineStatusRequest request) {
        return MySpotLineResponse.from(
            userSpotLineService.update(
                authUtil.requireUserId(),
                mySpotLineId,
                request.getStatus(),
                request.getScheduledDate()));
    }

    @Operation(summary = "내 SpotLine 삭제")
    @DeleteMapping("/users/me/spotlines/{mySpotLineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMySpotLine(@PathVariable UUID mySpotLineId) {
        userSpotLineService.delete(authUtil.requireUserId(), mySpotLineId);
    }

    @Operation(summary = "SpotLine 변형 목록")
    @GetMapping("/spotlines/{spotLineId}/variations")
    public SimplePageResponse<SpotLinePreviewResponse> getVariations(
            @PathVariable UUID spotLineId,
            @RequestParam(defaultValue = "0") int page) {
        SpotLine spotLine = spotLineRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<SpotLinePreviewResponse> all = spotLine.getVariations().stream()
            .filter(SpotLine::getIsActive)
            .map(SpotLinePreviewResponse::from)
            .toList();

        int pageSize = 20;
        int start = Math.min(page * pageSize, all.size());
        int end = Math.min(start + pageSize, all.size());
        return new SimplePageResponse<>(all.subList(start, end), end < all.size());
    }
}
