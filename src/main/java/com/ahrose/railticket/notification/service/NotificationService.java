package com.ahrose.railticket.notification.service;

import com.ahrose.railticket.bean.UserNotification;

import java.util.List;

/** 当前用户站内通知服务。 */
public interface NotificationService {

    /**
     * 查询当前用户的通知，支持只查看未读通知。
     *
     * @return {@code List<UserNotification>}，按创建时间倒序排列的通知列表
     */
    List<UserNotification> listNotifications(Long userId, boolean unreadOnly);

    /**
     * 查询当前用户未读通知数量。
     *
     * @return {@code long}，未读通知数量
     */
    long countUnreadNotifications(Long userId);

    /**
     * 将当前用户的一条通知标记为已读。
     *
     * @return {@code UserNotification}，标记后的通知
     */
    UserNotification markAsRead(Long userId, Long notificationId);

    /**
     * 将当前用户的全部未读通知标记为已读。
     *
     * @return {@code int}，本次更新的通知数量
     */
    int markAllAsRead(Long userId);

    /**
     * 删除当前用户的一条站内通知。
     *
     * @return 无返回值
     */
    void deleteNotification(Long userId, Long notificationId);
}
