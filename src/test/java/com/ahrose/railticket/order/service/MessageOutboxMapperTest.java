package com.ahrose.railticket.order.service;

import com.ahrose.railticket.bean.MessageOutbox;
import com.ahrose.railticket.common.enums.OutboxStatus;
import com.ahrose.railticket.order.mapper.MessageOutboxMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
class MessageOutboxMapperTest {

    @Autowired
    private MessageOutboxMapper outboxMapper;

    /** 验证同一条 Outbox 消息只能被一个发布任务原子抢占。 */
    @Test
    void claimForPublish_shouldAllowOnlyOneClaimWithinLease() {
        MessageOutbox outbox = insertNewOutbox();
        LocalDateTime now = LocalDateTime.now();
        String firstToken = UUID.randomUUID().toString();
        String secondToken = UUID.randomUUID().toString();

        int firstClaim = outboxMapper.claimForPublish(
                outbox.getId(), now, now.plusSeconds(30), firstToken
        );
        int secondClaim = outboxMapper.claimForPublish(
                outbox.getId(), now, now.plusSeconds(30), secondToken
        );

        MessageOutbox claimed = outboxMapper.selectById(outbox.getId());
        assertEquals(1, firstClaim);
        assertEquals(0, secondClaim);
        assertEquals(OutboxStatus.SENDING.name(), claimed.getStatus());
        assertEquals(firstToken, claimed.getClaimToken());
        printOutbox("Outbox 原子抢占测试", claimed);
    }

    /** 验证只有持有当前 claimToken 的发布任务才能把消息标记为 SENT。 */
    @Test
    void markSent_shouldRejectExpiredClaimToken() {
        MessageOutbox outbox = insertNewOutbox();
        LocalDateTime now = LocalDateTime.now();
        String validToken = UUID.randomUUID().toString();
        outboxMapper.claimForPublish(
                outbox.getId(), now, now.plusSeconds(30), validToken
        );

        int wrongResult = outboxMapper.markSent(
                outbox.getId(), LocalDateTime.now(), UUID.randomUUID().toString()
        );
        int correctResult = outboxMapper.markSent(
                outbox.getId(), LocalDateTime.now(), validToken
        );

        MessageOutbox sent = outboxMapper.selectById(outbox.getId());
        assertEquals(0, wrongResult);
        assertEquals(1, correctResult);
        assertEquals(OutboxStatus.SENT.name(), sent.getStatus());
        assertNull(sent.getClaimToken());
        printOutbox("Outbox 抢占令牌校验测试", sent);
    }

    private MessageOutbox insertNewOutbox() {
        MessageOutbox outbox = new MessageOutbox();
        outbox.setAggregateType("TICKET_ORDER");
        outbox.setAggregateId(IdWorker.getId());
        outbox.setEventType("ORDER_TIMEOUT");
        outbox.setPayload(outbox.getAggregateId().toString());
        outbox.setStatus(OutboxStatus.NEW.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        outboxMapper.insert(outbox);
        return outbox;
    }

    private void printOutbox(String testName, MessageOutbox outbox) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "outboxId=%d, status=%s, retryCount=%d, claimToken=%s%n",
                outbox.getId(),
                outbox.getStatus(),
                outbox.getRetryCount(),
                outbox.getClaimToken()
        );
        System.out.println("测试结果：通过");
    }
}
