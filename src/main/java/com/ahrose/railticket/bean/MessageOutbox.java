package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 与业务事务同时写入、随后可靠投递到消息队列的事务消息。 */
@Data
@TableName("message_outbox")
public class MessageOutbox {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String claimToken;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
