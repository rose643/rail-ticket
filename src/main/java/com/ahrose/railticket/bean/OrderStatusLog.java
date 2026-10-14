package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单状态流转日志。 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private LocalDateTime createdAt;
}
