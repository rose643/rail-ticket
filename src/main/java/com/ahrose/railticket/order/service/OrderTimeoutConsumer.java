package com.ahrose.railticket.order.service;

import com.ahrose.railticket.config.OrderRabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** 消费到期消息并幂等关闭待支付订单。 */
@Slf4j
@Component
public class OrderTimeoutConsumer {

    private final TicketOrderService orderService;

    public OrderTimeoutConsumer(TicketOrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = OrderRabbitConfig.CLOSE_QUEUE)
    public void closeExpiredOrder(String orderIdText) {
        try {
            orderService.closeExpiredOrder(Long.valueOf(orderIdText));
        } catch (NumberFormatException exception) {
            log.error("收到无法识别的订单超时消息，内容={}", orderIdText);
        }
    }
}
