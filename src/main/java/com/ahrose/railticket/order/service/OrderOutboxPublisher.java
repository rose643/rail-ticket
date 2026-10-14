package com.ahrose.railticket.order.service;

import com.ahrose.railticket.bean.MessageOutbox;
import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.common.enums.OrderStatus;
import com.ahrose.railticket.common.enums.OrderEventType;
import com.ahrose.railticket.common.enums.OutboxStatus;
import com.ahrose.railticket.config.OrderRabbitConfig;
import com.ahrose.railticket.order.event.OrderOutboxCreatedEvent;
import com.ahrose.railticket.order.mapper.MessageOutboxMapper;
import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 从共用事务消息表可靠发布订单超时消息和订单通知消息。 */
@Slf4j
@Component
public class OrderOutboxPublisher {

    private static final String AGGREGATE_TYPE = "TICKET_ORDER";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ERROR_LENGTH = 500;

    private final MessageOutboxMapper outboxMapper;
    private final TicketOrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;
    private final long claimTimeoutSeconds;

    public OrderOutboxPublisher(
            MessageOutboxMapper outboxMapper,
            TicketOrderMapper orderMapper,
            RabbitTemplate rabbitTemplate,
            @Value("${order.outbox.claim-timeout-seconds:30}") long claimTimeoutSeconds
    ) {
        this.outboxMapper = outboxMapper;
        this.orderMapper = orderMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.claimTimeoutSeconds = Math.max(10, claimTimeoutSeconds);
    }

    /** 订单事务成功提交后，通过专用线程池立即尝试发布当前消息。 */
    @Async("orderOutboxExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(OrderOutboxCreatedEvent event) {
        publishOne(event.outboxId());
    }

    /** 批量发布尚未成功投递的订单消息，并通过发布确认更新投递状态。 */
    @Scheduled(
            fixedDelayString = "${order.outbox.publish-interval-ms:5000}",
            initialDelayString = "${order.outbox.initial-delay-ms:5000}"
    )
    public void publishPendingMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<MessageOutbox> messages = outboxMapper.selectList(
                new LambdaQueryWrapper<MessageOutbox>()
                        .eq(MessageOutbox::getAggregateType, AGGREGATE_TYPE)
                        .in(MessageOutbox::getStatus,
                                OutboxStatus.NEW.name(),
                                OutboxStatus.FAILED.name(),
                                OutboxStatus.SENDING.name())
                        .le(MessageOutbox::getNextRetryAt, now)
                        .orderByAsc(MessageOutbox::getCreatedAt)
                        .last("LIMIT " + BATCH_SIZE)
        );
        messages.forEach(message -> publishOne(message.getId()));
    }

    private void publishOne(Long outboxId) {
        LocalDateTime now = LocalDateTime.now();
        String claimToken = UUID.randomUUID().toString();
        int claimed;
        try {
            claimed = outboxMapper.claimForPublish(
                    outboxId,
                    now,
                    now.plusSeconds(claimTimeoutSeconds),
                    claimToken
            );
        } catch (RuntimeException exception) {
            log.warn("抢占订单消息失败，outboxId={}", outboxId, exception);
            return;
        }
        if (claimed == 0) {
            return;
        }

        MessageOutbox outbox;
        try {
            outbox = outboxMapper.selectById(outboxId);
            if (outbox == null) {
                return;
            }
            publishClaimedMessage(outbox, claimToken);
        } catch (Exception exception) {
            markFailed(outboxId, claimToken, exception);
            log.warn("订单消息发布失败，outboxId={}", outboxId, exception);
        }
    }

    private void publishClaimedMessage(MessageOutbox outbox, String claimToken) throws Exception {
        OrderEventType eventType;
        try {
            eventType = OrderEventType.valueOf(outbox.getEventType());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("不支持的订单事件类型: " + outbox.getEventType(), exception);
        }

        if (eventType == OrderEventType.ORDER_TIMEOUT) {
            publishTimeoutMessage(outbox, claimToken);
            return;
        }
        if (eventType.isNotificationEvent()) {
            publishNotificationMessage(outbox, claimToken);
            return;
        }
        throw new IllegalStateException("没有可用的订单消息路由: " + eventType);
    }

    private void publishTimeoutMessage(MessageOutbox outbox, String claimToken) throws Exception {
        TicketOrder order = orderMapper.selectById(outbox.getAggregateId());
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            markSent(outbox.getId(), claimToken);
            return;
        }

        long delayMillis = Math.max(
                0L,
                Duration.between(LocalDateTime.now(), order.getExpireAt()).toMillis()
        );
        CorrelationData correlationData = new CorrelationData(outbox.getId().toString());

        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.DELAYED_EXCHANGE,
                OrderRabbitConfig.CLOSE_ROUTING_KEY,
                outbox.getPayload(),
                message -> {
                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(outbox.getId().toString());
                    message.getMessageProperties().setHeader(
                            OrderRabbitConfig.DELAY_HEADER,
                            delayMillis
                    );
                    return message;
                },
                correlationData
        );

        awaitPublisherConfirm(correlationData);
        markSent(outbox.getId(), claimToken);
    }

    private void publishNotificationMessage(MessageOutbox outbox, String claimToken) throws Exception {
        CorrelationData correlationData = new CorrelationData(outbox.getId().toString());
        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.NOTIFICATION_EXCHANGE,
                OrderRabbitConfig.NOTIFICATION_ROUTING_KEY,
                outbox.getPayload(),
                message -> {
                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(outbox.getId().toString());
                    message.getMessageProperties().setHeader(
                            OrderRabbitConfig.EVENT_TYPE_HEADER,
                            outbox.getEventType()
                    );
                    return message;
                },
                correlationData
        );
        awaitPublisherConfirm(correlationData);
        markSent(outbox.getId(), claimToken);
    }

    private void awaitPublisherConfirm(CorrelationData correlationData) throws Exception {
        CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ 未确认消息: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 无法路由消息");
        }
    }

    private void markSent(Long outboxId, String claimToken) {
        if (outboxMapper.markSent(outboxId, LocalDateTime.now(), claimToken) == 0) {
            throw new IllegalStateException("Outbox 发布状态已发生变化");
        }
    }

    private void markFailed(Long outboxId, String claimToken, Exception exception) {
        try {
            MessageOutbox outbox = outboxMapper.selectById(outboxId);
            int retryCount = outbox == null || outbox.getRetryCount() == null
                    ? 1
                    : outbox.getRetryCount() + 1;
            long retryDelaySeconds = Math.min(300L, 1L << Math.min(retryCount, 8));
            String error = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            outboxMapper.markFailed(
                    outboxId,
                    LocalDateTime.now().plusSeconds(retryDelaySeconds),
                    error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)),
                    claimToken
            );
        } catch (RuntimeException persistenceException) {
            log.error("记录 Outbox 发布失败状态异常，outboxId={}", outboxId, persistenceException);
        }
    }
}
