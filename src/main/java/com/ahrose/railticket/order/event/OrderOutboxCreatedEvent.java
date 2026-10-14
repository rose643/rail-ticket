package com.ahrose.railticket.order.event;

/** 订单事务中已经写入 Outbox 消息的领域事件。 */
public record OrderOutboxCreatedEvent(Long outboxId) {
}
