package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.UserNotification;

import java.time.LocalDateTime;

/** 站内通知响应。 */
public record UserNotificationResponse(
        Long id,
        String notificationType,
        String title,
        String content,
        String businessType,
        Long businessId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    /** 将数据库实体转换为不暴露内部接收人字段的接口响应。 */
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getBusinessType(),
                notification.getBusinessId(),
                Integer.valueOf(1).equals(notification.getReadStatus()),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
