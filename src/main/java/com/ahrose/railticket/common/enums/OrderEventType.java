package com.ahrose.railticket.common.enums;

/** 订单事务消息的事件类型。 */
public enum OrderEventType {

    /** 创建订单时发送的延迟关单消息，仅供超时消费者处理。 */
    ORDER_TIMEOUT,

    /** 待支付订单创建成功通知。 */
    ORDER_CREATED,

    /** 模拟支付成功通知。 */
    ORDER_PAID,

    /** 用户主动取消订单通知。 */
    ORDER_CANCELLED,

    /** 系统因支付超时关闭订单通知。 */
    ORDER_TIMEOUT_CLOSED,

    /** 模拟退票成功通知。 */
    ORDER_REFUNDED;

    /**
     * 判断当前事件是否需要立即投递给通知消费者。
     *
     * @return {@code boolean}，通知事件返回 true，延迟关单事件返回 false
     */
    public boolean isNotificationEvent() {
        return this != ORDER_TIMEOUT;
    }
}
