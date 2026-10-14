package com.ahrose.railticket.order.service;

import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.bean.response.TicketOrderResponse;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.order.mapper.OrderJourneyInfo;
import com.ahrose.railticket.order.mapper.OrderJourneyInfoMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 将订单实体批量装配为包含完整行程的接口响应。 */
@Component
public class TicketOrderResponseAssembler {

    private final OrderJourneyInfoMapper journeyInfoMapper;

    public TicketOrderResponseAssembler(OrderJourneyInfoMapper journeyInfoMapper) {
        this.journeyInfoMapper = journeyInfoMapper;
    }

    /** 装配单个订单响应。 */
    public TicketOrderResponse toResponse(TicketOrder order) {
        return toResponses(List.of(order)).getFirst();
    }

    /** 一次联表查询并按原顺序装配订单列表。 */
    public List<TicketOrderResponse> toResponses(List<TicketOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(TicketOrder::getId).toList();
        Map<Long, OrderJourneyInfo> journeyByOrderId = journeyInfoMapper.selectByOrderIds(orderIds)
                .stream()
                .collect(Collectors.toMap(OrderJourneyInfo::getOrderId, Function.identity()));

        return orders.stream()
                .map(order -> {
                    OrderJourneyInfo journey = journeyByOrderId.get(order.getId());
                    if (journey == null) {
                        throw new BusinessException(500, "订单关联的行程数据不完整");
                    }
                    return TicketOrderResponse.from(order, journey);
                })
                .toList();
    }
}
