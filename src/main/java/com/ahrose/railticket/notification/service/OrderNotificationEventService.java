package com.ahrose.railticket.notification.service;

import com.ahrose.railticket.order.event.OrderNotificationMessage;

/** 将订单 MQ 事件转换为一个或多个用户站内通知。 */
public interface OrderNotificationEventService {

    /**
     * 幂等消费订单通知事件。
     *
     * @return 无返回值；重复消息直接忽略
     */
    void consume(Long messageId, String eventType, OrderNotificationMessage message);
}
