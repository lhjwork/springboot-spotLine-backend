package com.spotline.api.domain.repository;

import com.spotline.api.domain.entity.Notification;
import com.spotline.api.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(String recipientId);

    boolean existsByActorIdAndTypeAndTargetTypeAndTargetIdAndCreatedAtAfter(
        String actorId, NotificationType type, String targetType, String targetId, LocalDateTime after);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsRead(@Param("recipientId") String recipientId);
}
