package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一期中一张订单对应一位乘车人的一张车票。 */
@Data
@TableName("ticket_order")
public class TicketOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long passengerId;
    private Long passengerUserId;
    private String passengerName;
    private String passengerIdType;
    private String passengerIdNo;
    private String passengerPhone;
    private Long scheduleId;
    private String seatTypeCode;
    private Integer fromStopIndex;
    private Integer toStopIndex;
    private BigDecimal amount;
    private String status;
    private String requestId;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
