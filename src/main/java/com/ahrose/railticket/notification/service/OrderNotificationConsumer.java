package com.ahrose.railticket.notification.service;

import com.ahrose.railticket.config.OrderRabbitConfig;
import com.ahrose.railticket.order.event.OrderNotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 接收订单状态事件并交给事务服务幂等生成站内通知。 */
@Slf4j
@Component
public class OrderNotificationConsumer {

    private final OrderNotificationEventService notificationEventService;
    private final ObjectMapper objectMapper;

    public OrderNotificationConsumer(
            OrderNotificationEventService notificationEventService,
            ObjectMapper objectMapper
    ) {
        this.notificationEventService = notificationEventService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = OrderRabbitConfig.NOTIFICATION_QUEUE)
    public void consume(Message message) {
        String messageIdText = message.getMessageProperties().getMessageId();
        Object eventTypeHeader = message.getMessageProperties()
                .getHeader(OrderRabbitConfig.EVENT_TYPE_HEADER);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            OrderNotificationMessage notificationMessage = objectMapper.readValue(
                    payload,
                    OrderNotificationMessage.class
            );
            notificationEventService.consume(
                    Long.valueOf(messageIdText),
                    eventTypeHeader == null ? null : eventTypeHeader.toString(),
                    notificationMessage
            );
        } catch (NumberFormatException | JsonProcessingException exception) {
            log.error(
                    "收到无法识别的订单通知消息，messageId={}, payload={}",
                    messageIdText,
                    payload
            );
            throw new IllegalArgumentException("订单通知消息格式错误", exception);
        }
    }
}
