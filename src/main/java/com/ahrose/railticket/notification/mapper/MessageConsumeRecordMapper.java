package com.ahrose.railticket.notification.mapper;

import com.ahrose.railticket.bean.MessageConsumeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** MQ 消费幂等记录数据库访问接口。 */
@Mapper
public interface MessageConsumeRecordMapper extends BaseMapper<MessageConsumeRecord> {

    /**
     * 原子登记消息消费权，重复消息由唯一索引转换为返回 0。
     *
     * @return {@code int}，1 表示首次消费，0 表示该消费者已经处理过此消息
     */
    @Insert("""
            INSERT IGNORE INTO message_consume_record (
                id, message_id, consumer_name, status, created_at, updated_at
            ) VALUES (
                #{id}, #{messageId}, #{consumerName}, 'PROCESSING', #{now}, #{now}
            )
            """)
    int insertIfAbsent(
            @Param("id") Long id,
            @Param("messageId") Long messageId,
            @Param("consumerName") String consumerName,
            @Param("now") LocalDateTime now
    );

    /** 将当前消费者的消息记录标记为处理成功。 */
    @Update("""
            UPDATE message_consume_record
            SET status = 'SUCCESS',
                consumed_at = #{consumedAt},
                updated_at = #{consumedAt}
            WHERE message_id = #{messageId}
              AND consumer_name = #{consumerName}
              AND status = 'PROCESSING'
            """)
    int markSuccess(
            @Param("messageId") Long messageId,
            @Param("consumerName") String consumerName,
            @Param("consumedAt") LocalDateTime consumedAt
    );
}
