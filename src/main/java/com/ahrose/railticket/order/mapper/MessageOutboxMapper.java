package com.ahrose.railticket.order.mapper;

import com.ahrose.railticket.bean.MessageOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 事务消息表数据库访问接口。 */
@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    /**
     * 原子抢占一条到期的待发送消息，并为本次发送设置处理租约。
     *
     * @param outboxId 消息雪花 ID
     * @param now 当前时间
     * @param claimUntil 本次发送租约截止时间
     * @param claimToken 本次抢占的唯一令牌
     * @return {@code int}，1 表示抢占成功，0 表示消息已被其他线程抢占或无需发送
     */
    @Update("""
            UPDATE message_outbox
            SET status = 'SENDING',
                next_retry_at = #{claimUntil},
                claim_token = #{claimToken},
                last_error = NULL
            WHERE id = #{outboxId}
              AND status IN ('NEW', 'FAILED', 'SENDING')
              AND next_retry_at <= #{now}
            """)
    int claimForPublish(
            @Param("outboxId") Long outboxId,
            @Param("now") LocalDateTime now,
            @Param("claimUntil") LocalDateTime claimUntil,
            @Param("claimToken") String claimToken
    );

    /** 发布确认成功后将抢占中的消息标记为已发送。 */
    @Update("""
            UPDATE message_outbox
            SET status = 'SENT',
                sent_at = #{sentAt},
                claim_token = NULL,
                last_error = NULL
            WHERE id = #{outboxId}
              AND status = 'SENDING'
              AND claim_token = #{claimToken}
            """)
    int markSent(
            @Param("outboxId") Long outboxId,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("claimToken") String claimToken
    );

    /** 发布失败后记录错误和下一次重试时间。 */
    @Update("""
            UPDATE message_outbox
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_at = #{nextRetryAt},
                claim_token = NULL,
                last_error = #{lastError}
            WHERE id = #{outboxId}
              AND status = 'SENDING'
              AND claim_token = #{claimToken}
            """)
    int markFailed(
            @Param("outboxId") Long outboxId,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("lastError") String lastError,
            @Param("claimToken") String claimToken
    );
}
