package com.spotline.api.controller;

import com.spotline.api.dto.request.ShareRequest;
import com.spotline.api.security.AuthUtil;
import com.spotline.api.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Share", description = "공유 추적 API")
@RestController
@RequestMapping("/api/v2/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final AuthUtil authUtil;

    @Operation(summary = "공유 이벤트 추적")
    @PostMapping
    public ResponseEntity<Map<String, Object>> trackShare(@Valid @RequestBody ShareRequest request) {
        String sharerId = authUtil.getCurrentUserId();
        int sharesCount = shareService.trackShare(request, sharerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "sharesCount", sharesCount));
    }
}
