package com.ahrose.railticket.notification.service;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.UserNotification;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.notification.mapper.UserNotificationMapper;
import com.ahrose.railticket.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserNotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    private SysUser currentUser;
    private SysUser anotherUser;

    /** 每个测试执行前准备两个用户，验证通知数据不会跨用户访问。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        currentUser = insertUser("notification_user_" + suffix);
        anotherUser = insertUser("notification_other_" + suffix);
    }

    /** 验证通知列表只返回当前用户的数据，并且可以仅查询未读通知。 */
    @Test
    void listNotifications_shouldFilterByUserAndUnreadStatus() {
        insertNotification(currentUser.getId(), "ORDER_CREATED", 0);
        insertNotification(currentUser.getId(), "ORDER_PAID", 1);
        insertNotification(anotherUser.getId(), "ORDER_REFUNDED", 0);

        List<UserNotification> all = notificationService.listNotifications(
                currentUser.getId(), false
        );
        List<UserNotification> unread = notificationService.listNotifications(
                currentUser.getId(), true
        );

        assertEquals(2, all.size());
        assertEquals(1, unread.size());
        assertEquals("ORDER_CREATED", unread.getFirst().getNotificationType());
        System.out.printf("当前用户通知数=%d，未读数=%d%n", all.size(), unread.size());
    }

    /** 验证未读数量只统计当前用户 readStatus=0 的通知。 */
    @Test
    void countUnreadNotifications_shouldCountCurrentUserOnly() {
        insertNotification(currentUser.getId(), "ORDER_CREATED", 0);
        insertNotification(currentUser.getId(), "ORDER_PAID", 0);
        insertNotification(currentUser.getId(), "ORDER_REFUNDED", 1);
        insertNotification(anotherUser.getId(), "ORDER_CREATED", 0);

        long unreadCount = notificationService.countUnreadNotifications(currentUser.getId());

        assertEquals(2L, unreadCount);
        System.out.println("当前用户未读通知数=" + unreadCount);
    }

    /** 验证将一条通知标记为已读时会同时记录阅读时间，重复操作保持幂等。 */
    @Test
    void markAsRead_shouldSetReadStatusAndRemainIdempotent() {
        UserNotification notification = insertNotification(
                currentUser.getId(), "ORDER_PAID", 0
        );

        UserNotification first = notificationService.markAsRead(
                currentUser.getId(), notification.getId()
        );
        UserNotification second = notificationService.markAsRead(
                currentUser.getId(), notification.getId()
        );

        assertEquals(1, first.getReadStatus());
        assertNotNull(first.getReadAt());
        assertEquals(first.getReadAt(), second.getReadAt());
        assertEquals(0L, notificationService.countUnreadNotifications(currentUser.getId()));
        System.out.println("通知已读时间=" + first.getReadAt());
    }

    /** 验证全部已读只修改当前用户的未读通知，不影响其他用户。 */
    @Test
    void markAllAsRead_shouldUpdateCurrentUserOnly() {
        insertNotification(currentUser.getId(), "ORDER_CREATED", 0);
        insertNotification(currentUser.getId(), "ORDER_PAID", 0);
        UserNotification anotherNotification = insertNotification(
                anotherUser.getId(), "ORDER_CREATED", 0
        );

        int updated = notificationService.markAllAsRead(currentUser.getId());

        assertEquals(2, updated);
        assertEquals(0L, notificationService.countUnreadNotifications(currentUser.getId()));
        assertEquals(0, notificationMapper.selectById(anotherNotification.getId()).getReadStatus());
        System.out.println("本次全部已读数量=" + updated);
    }

    /** 验证当前用户可以删除自己的通知，删除后数据库中不再存在该记录。 */
    @Test
    void deleteNotification_shouldDeleteOwnedNotification() {
        UserNotification notification = insertNotification(
                currentUser.getId(), "ORDER_CANCELLED", 0
        );

        notificationService.deleteNotification(currentUser.getId(), notification.getId());

        assertNull(notificationMapper.selectById(notification.getId()));
        System.out.println("已删除通知ID=" + notification.getId());
    }

    /** 验证用户不能读取、标记或删除其他用户的通知。 */
    @Test
    void notificationOperations_whenNotificationBelongsToAnotherUser_shouldReject() {
        UserNotification notification = insertNotification(
                anotherUser.getId(), "ORDER_REFUNDED", 0
        );

        BusinessException markException = assertThrows(
                BusinessException.class,
                () -> notificationService.markAsRead(currentUser.getId(), notification.getId())
        );
        BusinessException deleteException = assertThrows(
                BusinessException.class,
                () -> notificationService.deleteNotification(currentUser.getId(), notification.getId())
        );

        assertEquals(404, markException.getCode());
        assertEquals(404, deleteException.getCode());
        assertNotNull(notificationMapper.selectById(notification.getId()));
        System.out.println("跨用户操作已被拒绝");
    }

    private SysUser insertUser(String username) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private UserNotification insertNotification(
            Long userId,
            String notificationType,
            Integer readStatus
    ) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setNotificationType(notificationType);
        notification.setTitle("测试通知-" + notificationType);
        notification.setContent("这是一条通知服务测试数据");
        notification.setBusinessType("TICKET_ORDER");
        notification.setBusinessId(IdWorker.getId());
        notification.setEventId(IdWorker.getId());
        notification.setReadStatus(readStatus);
        notificationMapper.insert(notification);
        return notification;
    }
}
