package com.ahrose.railticket.order.service;

import com.ahrose.railticket.config.OrderRabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderRabbitConfigTest {

    @Autowired
    @Qualifier("orderTimeoutDelayedExchange")
    private CustomExchange delayedExchange;

    @Autowired
    @Qualifier("orderTimeoutCloseQueue")
    private Queue closeQueue;

    @Autowired
    @Qualifier("orderTimeoutDelayedBinding")
    private Binding delayedBinding;

    @Autowired
    @Qualifier("orderNotificationExchange")
    private DirectExchange notificationExchange;

    @Autowired
    @Qualifier("orderNotificationQueue")
    private Queue notificationQueue;

    @Autowired
    @Qualifier("orderNotificationBinding")
    private Binding notificationBinding;

    /** 验证订单超时交换机使用延迟消息插件类型，并以 direct 方式进行延迟后的路由。 */
    @Test
    void delayedExchange_shouldUseRabbitDelayedMessagePlugin() {
        assertEquals(OrderRabbitConfig.DELAYED_EXCHANGE, delayedExchange.getName());
        assertEquals("x-delayed-message", delayedExchange.getType());
        assertEquals("direct", delayedExchange.getArguments().get("x-delayed-type"));
        assertTrue(delayedExchange.isDurable());

        System.out.println("========== RabbitMQ 延迟交换机配置测试 ==========");
        System.out.println("交换机：" + delayedExchange.getName());
        System.out.println("类型：" + delayedExchange.getType());
        System.out.println("延迟路由类型：" + delayedExchange.getArguments().get("x-delayed-type"));
        System.out.println("测试结果：通过");
    }

    /** 验证延迟交换机使用指定路由键绑定到持久化的 quorum 关单队列。 */
    @Test
    void delayedBinding_shouldRouteToCloseQueue() {
        assertEquals(OrderRabbitConfig.CLOSE_QUEUE, closeQueue.getName());
        assertTrue(closeQueue.isDurable());
        assertEquals("quorum", closeQueue.getArguments().get("x-queue-type"));
        assertEquals(OrderRabbitConfig.DELAYED_EXCHANGE, delayedBinding.getExchange());
        assertEquals(OrderRabbitConfig.CLOSE_QUEUE, delayedBinding.getDestination());
        assertEquals(OrderRabbitConfig.CLOSE_ROUTING_KEY, delayedBinding.getRoutingKey());

        System.out.println("========== RabbitMQ 延迟队列绑定测试 ==========");
        System.out.println("交换机：" + delayedBinding.getExchange());
        System.out.println("队列：" + delayedBinding.getDestination());
        System.out.println("路由键：" + delayedBinding.getRoutingKey());
        System.out.println("测试结果：通过");
    }

    /** 验证订单通知使用持久化直连交换机，并绑定到持久化 quorum 通知队列。 */
    @Test
    void notificationBinding_shouldRouteToNotificationQueue() {
        assertEquals(OrderRabbitConfig.NOTIFICATION_EXCHANGE, notificationExchange.getName());
        assertEquals("direct", notificationExchange.getType());
        assertTrue(notificationExchange.isDurable());
        assertEquals(OrderRabbitConfig.NOTIFICATION_QUEUE, notificationQueue.getName());
        assertTrue(notificationQueue.isDurable());
        assertEquals("quorum", notificationQueue.getArguments().get("x-queue-type"));
        assertEquals(OrderRabbitConfig.NOTIFICATION_EXCHANGE, notificationBinding.getExchange());
        assertEquals(OrderRabbitConfig.NOTIFICATION_QUEUE, notificationBinding.getDestination());
        assertEquals(OrderRabbitConfig.NOTIFICATION_ROUTING_KEY, notificationBinding.getRoutingKey());

        System.out.println("========== RabbitMQ 通知队列绑定测试 ==========");
        System.out.println("交换机：" + notificationBinding.getExchange());
        System.out.println("队列：" + notificationBinding.getDestination());
        System.out.println("路由键：" + notificationBinding.getRoutingKey());
        System.out.println("测试结果：通过");
    }
}
