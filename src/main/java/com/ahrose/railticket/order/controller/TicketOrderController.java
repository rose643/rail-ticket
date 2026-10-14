package com.ahrose.railticket.order.controller;

import com.ahrose.railticket.bean.request.TicketOrderCreateRequest;
import com.ahrose.railticket.bean.response.TicketOrderResponse;
import com.ahrose.railticket.common.result.Result;
import com.ahrose.railticket.order.service.TicketOrderService;
import com.ahrose.railticket.order.service.TicketOrderResponseAssembler;
import com.ahrose.railticket.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前用户的购票订单和本人车票接口。 */
@RestController
@RequestMapping("/api")
public class TicketOrderController {

    private final TicketOrderService orderService;
    private final TicketOrderResponseAssembler responseAssembler;

    public TicketOrderController(TicketOrderService orderService, TicketOrderResponseAssembler responseAssembler) {
        this.orderService = orderService;
        this.responseAssembler = responseAssembler;
    }

    /** 创建一张待支付车票订单，返回服务端价格和15分钟支付期限。 */
    @PostMapping("/orders")
    public Result<TicketOrderResponse> createOrder(
            @CurrentUserId Long userId,
            @Valid @RequestBody TicketOrderCreateRequest request
    ) {
        return Result.success(responseAssembler.toResponse(orderService.createOrder(userId, request)));
    }

    /** 查询当前用户作为购票人的全部订单。 */
    @GetMapping("/orders")
    public Result<List<TicketOrderResponse>> listPurchasedOrders(@CurrentUserId Long userId) {
        return Result.success(responseAssembler.toResponses(orderService.listPurchasedOrders(userId)));
    }

    /** 查询当前用户作为实际乘车人的全部本人车票。 */
    @GetMapping("/tickets/me")
    public Result<List<TicketOrderResponse>> listMyTickets(@CurrentUserId Long userId) {
        return Result.success(responseAssembler.toResponses(orderService.listMyTickets(userId)));
    }

    /** 购票人或实际乘车人查询订单详情。 */
    @GetMapping("/orders/{orderId}")
    public Result<TicketOrderResponse> getOrder(
            @CurrentUserId Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(responseAssembler.toResponse(orderService.getVisibleOrder(userId, orderId)));
    }

    /** 购票人取消待支付订单并恢复区段库存。 */
    @PostMapping("/orders/{orderId}/cancel")
    public Result<TicketOrderResponse> cancelOrder(
            @CurrentUserId Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(responseAssembler.toResponse(orderService.cancelOrder(userId, orderId)));
    }

    /** 购票人模拟支付待支付订单。 */
    @PostMapping("/orders/{orderId}/pay")
    public Result<TicketOrderResponse> payOrder(
            @CurrentUserId Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(responseAssembler.toResponse(orderService.payOrder(userId, orderId)));
    }

    /** 购票人模拟退票并恢复区段库存。 */
    @PostMapping("/orders/{orderId}/refund")
    public Result<TicketOrderResponse> refundOrder(
            @CurrentUserId Long userId,
            @PathVariable Long orderId
    ) {
        return Result.success(responseAssembler.toResponse(orderService.refundOrder(userId, orderId)));
    }
}
