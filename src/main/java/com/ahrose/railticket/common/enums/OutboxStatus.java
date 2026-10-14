package com.ahrose.railticket.common.enums;

/** 事务消息发送状态。 */
public enum OutboxStatus {
    NEW,
    SENDING,
    SENT,
    FAILED
}
