package com.ahrose.railticket.order.service;

import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.bean.request.TicketOrderCreateRequest;

import java.util.List;

/** 车票订单业务接口。 */
public interface TicketOrderService {

    /**
     * 创建待支付订单，并在同一事务中扣减全部乘车区段库存。
     *
     * @param userId 当前购票用户雪花 ID
     * @param request 创建订单请求
     * @return {@code TicketOrder}，创建成功或同一幂等请求已经创建的订单
     */
    TicketOrder createOrder(Long userId, TicketOrderCreateRequest request);

    /**
     * 查询当前用户作为购票人创建的订单。
     *
     * @param userId 当前用户雪花 ID
     * @return {@code List<TicketOrder>}，当前用户购买的订单列表
     */
    List<TicketOrder> listPurchasedOrders(Long userId);

    /**
     * 查询当前用户作为实际乘车人的本人车票；历史未关联订单使用实名认证快照兜底匹配。
     *
     * @param userId 当前用户雪花 ID
     * @return {@code List<TicketOrder>}，实名信息与当前账号匹配的车票列表
     */
    List<TicketOrder> listMyTickets(Long userId);

    /**
     * 查询当前用户有权查看的订单；购票人、已关联乘车人以及实名快照匹配的乘车人均可查看。
     *
     * @param userId 当前用户雪花 ID
     * @param orderId 订单雪花 ID
     * @return {@code TicketOrder}，指定订单
     */
    TicketOrder getVisibleOrder(Long userId, Long orderId);

    /**
     * 购票人取消待支付订单并恢复全部区段库存。
     *
     * @param userId 当前购票用户雪花 ID
     * @param orderId 订单雪花 ID
     * @return {@code TicketOrder}，取消后的订单
     */
    TicketOrder cancelOrder(Long userId, Long orderId);

    /**
     * 购票人模拟支付未过期的待支付订单。
     *
     * @param userId 当前购票用户雪花 ID
     * @param orderId 订单雪花 ID
     * @return {@code TicketOrder}，支付后的订单
     */
    TicketOrder payOrder(Long userId, Long orderId);

    /**
     * 购票人对已支付订单模拟退票，并恢复全部区段库存。
     *
     * @param userId 当前购票用户雪花 ID
     * @param orderId 订单雪花 ID
     * @return {@code TicketOrder}，退票后的订单
     */
    TicketOrder refundOrder(Long userId, Long orderId);

    /**
     * MQ 消费者和数据库补偿任务共用的幂等超时关单方法。
     *
     * @param orderId 订单雪花 ID
     * @return 无返回值；订单不是已到期待支付状态时直接结束
     */
    void closeExpiredOrder(Long orderId);
}
