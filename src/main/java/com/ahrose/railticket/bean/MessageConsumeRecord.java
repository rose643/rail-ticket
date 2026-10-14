package com.ahrose.railticket.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** MQ 消费者处理消息的幂等记录。 */
@Data
@TableName("message_consume_record")
public class MessageConsumeRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long messageId;
    private String consumerName;
    private String status;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
