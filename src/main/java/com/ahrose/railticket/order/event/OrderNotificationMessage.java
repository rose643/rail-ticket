package com.ahrose.railticket.order.event;

import com.ahrose.railticket.bean.TicketOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 通知消费者所需的订单事件快照，不包含手机号、证件号等敏感信息。 */
public record OrderNotificationMessage(
        Long orderId,
        String orderNo,
        Long buyerUserId,
        Long passengerUserId,
        BigDecimal amount,
        LocalDateTime expireAt,
        LocalDateTime occurredAt
) {

    /** 从订单实体生成当前事件发生时的不可变消息快照。 */
    public static OrderNotificationMessage from(TicketOrder order, LocalDateTime occurredAt) {
        return new OrderNotificationMessage(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getPassengerUserId(),
                order.getAmount(),
                order.getExpireAt(),
                occurredAt
        );
    }
}
