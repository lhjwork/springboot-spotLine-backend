package com.spotline.api.controller;

import com.spotline.api.dto.response.NotificationResponse;
import com.spotline.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public Page<NotificationResponse> getNotifications(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.getNotifications(userId, PageRequest.of(page, Math.min(size, 50)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 수 조회")
    public Map<String, Long> getUnreadCount(@RequestAttribute("userId") String userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리")
    public void markAsRead(@RequestAttribute("userId") String userId, @PathVariable UUID id) {
        notificationService.markAsRead(userId, id);
    }

    @PutMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리")
    public Map<String, Integer> markAllAsRead(@RequestAttribute("userId") String userId) {
        return Map.of("updated", notificationService.markAllAsRead(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제")
    public void deleteNotification(@RequestAttribute("userId") String userId, @PathVariable UUID id) {
        notificationService.delete(userId, id);
    }
}
