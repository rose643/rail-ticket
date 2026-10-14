package com.ahrose.railticket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/** 基于 RabbitMQ 延迟消息插件的订单超时队列配置。 */
@Configuration
@EnableScheduling
public class OrderRabbitConfig {

    /** x-delayed-message 交换机名称。 */
    public static final String DELAYED_EXCHANGE = "order.timeout.delayed.exchange";

    /** 到期订单关单队列名称。 */
    public static final String CLOSE_QUEUE = "order.timeout.close.queue";

    /** 延迟交换机向关单队列投递消息使用的路由键。 */
    public static final String CLOSE_ROUTING_KEY = "order.timeout.close";

    /** RabbitMQ 延迟消息插件识别的消息头。 */
    public static final String DELAY_HEADER = "x-delay";

    /** 订单状态通知使用的普通直连交换机。 */
    public static final String NOTIFICATION_EXCHANGE = "order.notification.exchange";

    /** 订单通知消费队列。 */
    public static final String NOTIFICATION_QUEUE = "order.notification.queue";

    /** 订单通知交换机向通知队列投递消息使用的路由键。 */
    public static final String NOTIFICATION_ROUTING_KEY = "order.notification";

    /** 消息中保存订单事件类型的请求头名称。 */
    public static final String EVENT_TYPE_HEADER = "order-event-type";

    @Bean
    public CustomExchange orderTimeoutDelayedExchange() {
        return new CustomExchange(
                DELAYED_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    @Bean
    public Queue orderTimeoutCloseQueue() {
        return QueueBuilder.durable(CLOSE_QUEUE)
                .quorum()
                .build();
    }

    @Bean
    public Binding orderTimeoutDelayedBinding() {
        return BindingBuilder.bind(orderTimeoutCloseQueue())
                .to(orderTimeoutDelayedExchange())
                .with(CLOSE_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public DirectExchange orderNotificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .quorum()
                .build();
    }

    @Bean
    public Binding orderNotificationBinding() {
        return BindingBuilder.bind(orderNotificationQueue())
                .to(orderNotificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }
}
