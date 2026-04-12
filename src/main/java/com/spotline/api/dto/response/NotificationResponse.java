package com.spotline.api.dto.response;

import com.spotline.api.domain.entity.Notification;
import com.spotline.api.domain.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String targetType;
    private String targetId;
    private String targetSlug;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String actorId;
    private String actorNickname;
    private String actorAvatar;

    public static NotificationResponse from(Notification n, User actor) {
        return NotificationResponse.builder()
            .id(n.getId())
            .type(n.getType().name())
            .targetType(n.getTargetType())
            .targetId(n.getTargetId())
            .targetSlug(n.getTargetSlug())
            .isRead(n.getIsRead())
            .createdAt(n.getCreatedAt())
            .actorId(n.getActorId())
            .actorNickname(actor != null ? actor.getNickname() : "알 수 없음")
            .actorAvatar(actor != null ? actor.getAvatar() : null)
            .build();
    }
}
