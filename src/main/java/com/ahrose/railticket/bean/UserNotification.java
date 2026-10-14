package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户收到的站内通知。 */
@Data
@TableName("user_notification")
public class UserNotification {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String notificationType;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private Long eventId;
    private Integer readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
