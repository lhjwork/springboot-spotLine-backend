package com.spotline.api.service;

import com.spotline.api.domain.entity.Notification;
import com.spotline.api.domain.entity.User;
import com.spotline.api.domain.enums.NotificationType;
import com.spotline.api.domain.repository.NotificationRepository;
import com.spotline.api.domain.repository.UserRepository;
import com.spotline.api.dto.response.NotificationResponse;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void create(String actorId, String recipientId, NotificationType type,
                       String targetType, String targetId, String targetSlug) {
        if (actorId.equals(recipientId)) return;

        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        if (notificationRepository.existsByActorIdAndTypeAndTargetTypeAndTargetIdAndCreatedAtAfter(
                actorId, type, targetType, targetId, fiveMinAgo)) {
            return;
        }

        notificationRepository.save(Notification.builder()
            .recipientId(recipientId)
            .actorId(actorId)
            .type(type)
            .targetType(targetType)
            .targetId(targetId)
            .targetSlug(targetSlug)
            .build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
            .map(n -> {
                User actor = userRepository.findById(n.getActorId()).orElse(null);
                return NotificationResponse.from(n, actor);
            });
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public void markAsRead(String userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"));
        if (!n.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    public void delete(String userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"));
        if (!n.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 알림만 삭제할 수 있습니다");
        }
        notificationRepository.delete(n);
    }
}
