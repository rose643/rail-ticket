package com.ahrose.railticket.notification.service;

import com.ahrose.railticket.bean.UserNotification;
import com.ahrose.railticket.common.enums.OrderEventType;
import com.ahrose.railticket.notification.mapper.MessageConsumeRecordMapper;
import com.ahrose.railticket.notification.mapper.UserNotificationMapper;
import com.ahrose.railticket.notification.service.impl.OrderNotificationEventServiceImpl;
import com.ahrose.railticket.order.event.OrderNotificationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationEventServiceTest {

    @Mock
    private MessageConsumeRecordMapper consumeRecordMapper;

    @Mock
    private UserNotificationMapper notificationMapper;

    private OrderNotificationEventService service;

    @BeforeEach
    void setUp() {
        service = new OrderNotificationEventServiceImpl(
                consumeRecordMapper,
                notificationMapper
        );
    }

    /** 验证支付事件会分别为购票人和已注册的实际乘车人生成站内通知。 */
    @Test
    void consumePaidEvent_shouldNotifyBuyerAndPassenger() {
        long messageId = 9001L;
        OrderNotificationMessage message = buildMessage(1001L, 1002L);
        when(consumeRecordMapper.insertIfAbsent(
                anyLong(), eq(messageId), eq("order-notification-consumer"), any()
        )).thenReturn(1);
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);
        when(consumeRecordMapper.markSuccess(
                eq(messageId), eq("order-notification-consumer"), any()
        )).thenReturn(1);

        service.consume(messageId, OrderEventType.ORDER_PAID.name(), message);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<UserNotification> notifications = captor.getAllValues();
        assertEquals(List.of(1001L, 1002L), notifications.stream()
                .map(UserNotification::getUserId)
                .toList());
        assertTrue(notifications.stream().allMatch(notification ->
                notification.getEventId().equals(messageId)
                        && notification.getNotificationType().equals("ORDER_PAID")
                        && notification.getReadStatus().equals(0)
        ));
        verify(consumeRecordMapper).markSuccess(
                eq(messageId), eq("order-notification-consumer"), any()
        );
    }

    /** 验证购票人与乘车人是同一账号时只生成一条通知。 */
    @Test
    void consumeCreatedEvent_whenBuyerIsPassenger_shouldNotifyOnce() {
        long messageId = 9002L;
        OrderNotificationMessage message = buildMessage(1001L, 1001L);
        when(consumeRecordMapper.insertIfAbsent(anyLong(), anyLong(), any(), any()))
                .thenReturn(1);
        when(notificationMapper.insert(any(UserNotification.class))).thenReturn(1);
        when(consumeRecordMapper.markSuccess(anyLong(), any(), any())).thenReturn(1);

        service.consume(messageId, OrderEventType.ORDER_CREATED.name(), message);

        verify(notificationMapper).insert(any(UserNotification.class));
    }

    /** 验证重复投递的消息无法再次取得消费权，也不会重复查询订单或生成通知。 */
    @Test
    void consume_whenMessageAlreadyConsumed_shouldIgnoreDuplicate() {
        long messageId = 9003L;
        when(consumeRecordMapper.insertIfAbsent(
                anyLong(), eq(messageId), eq("order-notification-consumer"), any()
        )).thenReturn(0);

        service.consume(
                messageId,
                OrderEventType.ORDER_REFUNDED.name(),
                buildMessage(1001L, 1002L)
        );

        verify(notificationMapper, never()).insert(any(UserNotification.class));
        verify(consumeRecordMapper, never()).markSuccess(anyLong(), any(), any());
    }

    private OrderNotificationMessage buildMessage(Long buyerId, Long passengerUserId) {
        LocalDateTime now = LocalDateTime.now();
        return new OrderNotificationMessage(
                2001L,
                "RT2001",
                buyerId,
                passengerUserId,
                new BigDecimal("553.00"),
                now.plusMinutes(15),
                now
        );
    }
}
