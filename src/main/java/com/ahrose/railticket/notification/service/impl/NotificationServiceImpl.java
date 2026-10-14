package com.ahrose.railticket.notification.service.impl;

import com.ahrose.railticket.bean.UserNotification;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.notification.mapper.UserNotificationMapper;
import com.ahrose.railticket.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 当前用户站内通知的查询和已读状态管理实现。 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final UserNotificationMapper notificationMapper;

    public NotificationServiceImpl(UserNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotification> listNotifications(Long userId, boolean unreadOnly) {
        return notificationMapper.selectList(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(unreadOnly, UserNotification::getReadStatus, 0)
                        .orderByDesc(UserNotification::getCreatedAt)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getReadStatus, 0)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserNotification markAsRead(Long userId, Long notificationId) {
        UserNotification notification = getOwnedNotificationOrThrow(userId, notificationId);
        if (Integer.valueOf(1).equals(notification.getReadStatus())) {
            return notification;
        }
        notification.setReadStatus(1);
        notification.setReadAt(LocalDateTime.now());
        if (notificationMapper.updateById(notification) == 0) {
            throw new BusinessException(409, "通知状态已发生变化，请刷新后重试");
        }
        return notificationMapper.selectById(notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return notificationMapper.update(
                new LambdaUpdateWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getReadStatus, 0)
                        .set(UserNotification::getReadStatus, 1)
                        .set(UserNotification::getReadAt, now)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long notificationId) {
        int deleted = notificationMapper.delete(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getId, notificationId)
                        .eq(UserNotification::getUserId, userId)
        );
        if (deleted == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    private UserNotification getOwnedNotificationOrThrow(Long userId, Long notificationId) {
        UserNotification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getId, notificationId)
                        .eq(UserNotification::getUserId, userId)
        );
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return notification;
    }
}
