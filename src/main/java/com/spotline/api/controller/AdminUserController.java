package com.spotline.api.controller;

import com.spotline.api.dto.response.UserAdminResponse;
import com.spotline.api.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin - User", description = "관리자 유저 관리")
@RestController
@RequestMapping("/api/v2/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 목록 (관리자)")
    @GetMapping
    public Page<UserAdminResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return adminUserService.list(status, keyword, pageable);
    }

    @Operation(summary = "유저 정지")
    @PatchMapping("/{userId}/suspend")
    public UserAdminResponse suspend(
            @PathVariable String userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return adminUserService.suspend(userId, reason);
    }

    @Operation(summary = "유저 정지 해제")
    @PatchMapping("/{userId}/unsuspend")
    public UserAdminResponse unsuspend(@PathVariable String userId) {
        return adminUserService.unsuspend(userId);
    }
}
