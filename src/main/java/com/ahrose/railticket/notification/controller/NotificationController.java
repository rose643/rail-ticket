package com.ahrose.railticket.notification.controller;

import com.ahrose.railticket.bean.response.UserNotificationResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.notification.service.NotificationService;
import com.ahrose.railticket.security.CurrentUserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前登录用户的站内通知接口。 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 查询当前用户的通知列表，unreadOnly=true 时仅返回未读通知。 */
    @GetMapping
    public Result<List<UserNotificationResponse>> listNotifications(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return Result.success(notificationService.listNotifications(userId, unreadOnly)
                .stream()
                .map(UserNotificationResponse::from)
                .toList());
    }

    /** 查询当前用户的未读通知数量。 */
    @GetMapping("/unread-count")
    public Result<Long> countUnreadNotifications(@CurrentUserId Long userId) {
        return Result.success(notificationService.countUnreadNotifications(userId));
    }

    /** 将当前用户指定的一条通知标记为已读。 */
    @PatchMapping("/{notificationId}/read")
    public Result<UserNotificationResponse> markAsRead(
            @CurrentUserId Long userId,
            @PathVariable Long notificationId
    ) {
        return Result.success(UserNotificationResponse.from(
                notificationService.markAsRead(userId, notificationId)
        ));
    }

    /** 将当前用户的全部未读通知标记为已读，返回更新数量。 */
    @PatchMapping("/read-all")
    public Result<Integer> markAllAsRead(@CurrentUserId Long userId) {
        return Result.success(notificationService.markAllAsRead(userId));
    }

    /** 删除当前用户指定的一条站内通知。 */
    @DeleteMapping("/{notificationId}")
    public Result<Void> deleteNotification(
            @CurrentUserId Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(userId, notificationId);
        return Result.success();
    }
}
