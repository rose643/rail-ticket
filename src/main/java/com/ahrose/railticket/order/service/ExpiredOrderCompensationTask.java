package com.ahrose.railticket.order.service;

import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 在消息丢失或队列长时间不可用时补偿关闭到期订单。 */
@Slf4j
@Component
public class ExpiredOrderCompensationTask {

    private static final int BATCH_SIZE = 100;

    private final TicketOrderMapper orderMapper;
    private final TicketOrderService orderService;

    public ExpiredOrderCompensationTask(
            TicketOrderMapper orderMapper,
            TicketOrderService orderService
    ) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Scheduled(
            fixedDelayString = "${order.timeout.scan-interval-ms:60000}",
            initialDelayString = "${order.timeout.scan-initial-delay-ms:60000}"
    )
    public void closeExpiredOrders() {
        for (Long orderId : orderMapper.selectExpiredPendingOrderIds(BATCH_SIZE)) {
            try {
                orderService.closeExpiredOrder(orderId);
            } catch (RuntimeException exception) {
                log.error("补偿关闭超时订单失败，orderId={}", orderId, exception);
            }
        }
    }
}
