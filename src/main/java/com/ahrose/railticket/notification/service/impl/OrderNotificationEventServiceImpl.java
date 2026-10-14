package com.ahrose.railticket.notification.service.impl;

import com.ahrose.railticket.bean.UserNotification;
import com.ahrose.railticket.common.enums.OrderEventType;
import com.ahrose.railticket.notification.mapper.MessageConsumeRecordMapper;
import com.ahrose.railticket.notification.mapper.UserNotificationMapper;
import com.ahrose.railticket.notification.service.OrderNotificationEventService;
import com.ahrose.railticket.order.event.OrderNotificationMessage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** 通过 Inbox 幂等记录生成购票人和实际乘车人的订单通知。 */
@Service
public class OrderNotificationEventServiceImpl implements OrderNotificationEventService {

    private static final String CONSUMER_NAME = "order-notification-consumer";
    private static final String BUSINESS_TYPE = "TICKET_ORDER";

    private final MessageConsumeRecordMapper consumeRecordMapper;
    private final UserNotificationMapper notificationMapper;

    public OrderNotificationEventServiceImpl(
            MessageConsumeRecordMapper consumeRecordMapper,
            UserNotificationMapper notificationMapper
    ) {
        this.consumeRecordMapper = consumeRecordMapper;
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(
            Long messageId,
            String eventTypeText,
            OrderNotificationMessage message
    ) {
        OrderEventType eventType = parseNotificationEvent(eventTypeText);
        validateMessage(message); // 校验
        LocalDateTime now = LocalDateTime.now();
        int acquired = consumeRecordMapper.insertIfAbsent(
                IdWorker.getId(), messageId, CONSUMER_NAME, now
        );
        if (acquired == 0) {
            return;
        }

        NotificationText text = buildNotificationText(eventType, message);
        for (Long recipientId : resolveRecipientIds(message)) {
            UserNotification notification = new UserNotification();
            notification.setUserId(recipientId);
            notification.setNotificationType(eventType.name());
            notification.setTitle(text.title());
            notification.setContent(text.content());
            notification.setBusinessType(BUSINESS_TYPE);
            notification.setBusinessId(message.orderId());
            notification.setEventId(messageId);
            notification.setReadStatus(0);
            if (notificationMapper.insert(notification) == 0) {
                throw new IllegalStateException("创建用户站内通知失败");
            }
        }

        if (consumeRecordMapper.markSuccess(messageId, CONSUMER_NAME, LocalDateTime.now()) == 0) {
            throw new IllegalStateException("更新消息消费状态失败: " + messageId);
        }
    }

    private void validateMessage(OrderNotificationMessage message) {
        if (message == null
                || message.orderId() == null
                || message.orderNo() == null
                || message.buyerUserId() == null) {
            throw new IllegalArgumentException("订单通知消息缺少必要字段");
        }
    }

    private OrderEventType parseNotificationEvent(String eventTypeText) {
        OrderEventType eventType;
        try {
            eventType = OrderEventType.valueOf(eventTypeText);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("无法识别的订单通知事件: " + eventTypeText, exception);
        }
        if (!eventType.isNotificationEvent()) {
            throw new IllegalArgumentException("延迟关单事件不能由通知消费者处理");
        }
        return eventType;
    }

    private Set<Long> resolveRecipientIds(OrderNotificationMessage message) {
        Set<Long> recipientIds = new LinkedHashSet<>();
        recipientIds.add(message.buyerUserId());
        if (message.passengerUserId() != null) {
            recipientIds.add(message.passengerUserId());
        }
        return recipientIds;
    }

    private NotificationText buildNotificationText(
            OrderEventType eventType,
            OrderNotificationMessage message
    ) {
        String orderNo = message.orderNo();
        return switch (eventType) {
            case ORDER_CREATED -> new NotificationText(
                    "订单创建成功",
                    "订单 " + orderNo + " 已创建，购票人需在 "
                            + message.expireAt() + " 前完成支付。"
            );
            case ORDER_PAID -> new NotificationText(
                    "购票成功",
                    "订单 " + orderNo + " 已支付成功，支付金额为 ¥" + message.amount() + "。"
            );
            case ORDER_CANCELLED -> new NotificationText(
                    "订单已取消",
                    "订单 " + orderNo + " 已由购票人取消。"
            );
            case ORDER_TIMEOUT_CLOSED -> new NotificationText(
                    "订单超时关闭",
                    "订单 " + orderNo + " 因超过支付期限已自动关闭。"
            );
            case ORDER_REFUNDED -> new NotificationText(
                    "退票成功",
                    "订单 " + orderNo + " 已退票，退款金额为 ¥" + message.amount() + "。"
            );
            case ORDER_TIMEOUT -> throw new IllegalArgumentException("延迟关单事件不生成通知");
        };
    }

    private record NotificationText(String title, String content) {
    }
}
