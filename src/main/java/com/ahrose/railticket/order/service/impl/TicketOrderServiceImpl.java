package com.ahrose.railticket.order.service.impl;

import com.ahrose.railticket.bean.MessageOutbox;
import com.ahrose.railticket.bean.OrderStatusLog;
import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.UserIdentity;
import com.ahrose.railticket.bean.request.TicketOrderCreateRequest;
import com.ahrose.railticket.common.enums.OrderStatus;
import com.ahrose.railticket.common.enums.OrderEventType;
import com.ahrose.railticket.common.enums.OutboxStatus;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.identity.service.UserIdentityService;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.order.event.OrderOutboxCreatedEvent;
import com.ahrose.railticket.order.event.OrderNotificationMessage;
import com.ahrose.railticket.order.mapper.MessageOutboxMapper;
import com.ahrose.railticket.order.mapper.OrderCreateContext;
import com.ahrose.railticket.order.mapper.OrderCreateContextMapper;
import com.ahrose.railticket.order.mapper.OrderStatusLogMapper;
import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import com.ahrose.railticket.order.service.OrderRequestGuard;
import com.ahrose.railticket.order.service.TicketOrderService;
import com.ahrose.railticket.passenger.service.PassengerService;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.ahrose.railticket.train.search.service.TrainSearchCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** 订单创建、状态流转和区段库存一致性实现。 */
@Service
public class TicketOrderServiceImpl implements TicketOrderService {

    private static final String AGGREGATE_TYPE = "TICKET_ORDER";

    private final TicketOrderMapper orderMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final MessageOutboxMapper outboxMapper;
    private final SeatInventoryMapper inventoryMapper;
    private final OrderCreateContextMapper orderCreateContextMapper;
    private final TrainScheduleMapper scheduleMapper;
    private final TrainStopMapper trainStopMapper;
    private final PassengerService passengerService;
    private final UserIdentityService identityService;
    private final OrderRequestGuard requestGuard;
    private final TrainSearchCacheService searchCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final long paymentTimeoutMinutes;

    public TicketOrderServiceImpl(
            TicketOrderMapper orderMapper,
            OrderStatusLogMapper statusLogMapper,
            MessageOutboxMapper outboxMapper,
            SeatInventoryMapper inventoryMapper,
            OrderCreateContextMapper orderCreateContextMapper,
            TrainScheduleMapper scheduleMapper,
            TrainStopMapper trainStopMapper,
            PassengerService passengerService,
            UserIdentityService identityService,
            OrderRequestGuard requestGuard,
            TrainSearchCacheService searchCacheService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${order.payment-timeout-minutes:15}") long paymentTimeoutMinutes
    ) {
        this.orderMapper = orderMapper;
        this.statusLogMapper = statusLogMapper;
        this.outboxMapper = outboxMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderCreateContextMapper = orderCreateContextMapper;
        this.scheduleMapper = scheduleMapper;
        this.trainStopMapper = trainStopMapper;
        this.passengerService = passengerService;
        this.identityService = identityService;
        this.requestGuard = requestGuard;
        this.searchCacheService = searchCacheService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        if (paymentTimeoutMinutes <= 0) {
            throw new IllegalArgumentException("订单支付超时时间必须大于 0 分钟");
        }
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder createOrder(Long userId, TicketOrderCreateRequest request) {
        String requestId = request.requestId().trim();
        TicketOrder existing = findByRequestId(userId, requestId);
        if (existing != null) {
            return existing;
        }
        if (!requestGuard.tryAcquire(userId, requestId)) {
            existing = findByRequestId(userId, requestId);
            if (existing != null) {
                return existing;
            }
            throw new BusinessException(409, "相同下单请求正在处理中，请勿重复提交");
        }

        String seatTypeCode = request.seatTypeCode().trim().toUpperCase(Locale.ROOT);
        validateRouteAndSeat(request.fromStopIndex(), request.toStopIndex(), seatTypeCode);  // 校验

        Passenger passenger = passengerService.getPassenger(userId, request.passengerId());
        OrderCreateContext context = orderCreateContextMapper.selectOrderCreateContext(
                request.scheduleId(),
                seatTypeCode,
                request.fromStopIndex(),
                request.toStopIndex()
        );
        validateOrderCreateContext(context);  //校验

        UserIdentity passengerIdentity = identityService.findVerifiedIdentity(
                passenger.getIdType(),
                passenger.getIdNo()
        );
        if (passengerIdentity != null
                && !passengerIdentity.getRealName().equals(passenger.getName())) {
            throw new BusinessException(409, "乘车人姓名与实名认证信息不一致");
        }

        LocalDateTime now = LocalDateTime.now();
        TicketOrder order = buildOrder(
                userId,
                passenger,
                passengerIdentity,
                request,
                seatTypeCode,
                context,
                now
        );
        try {
            if (orderMapper.insert(order) == 0) {
                throw new BusinessException(500, "创建订单失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "相同下单请求已经提交，请查询订单结果");
        }

        deductInventory(order);   // 校验
        insertStatusLog(order.getId(), null, OrderStatus.PENDING_PAYMENT, "创建订单");   // 插入订单状态日志
        insertOrderOutbox(order, OrderEventType.ORDER_TIMEOUT, now);   // 插入延迟关单消息
        insertOrderOutbox(order, OrderEventType.ORDER_CREATED, now);   // 插入下单通知消息
        searchCacheService.evictSegmentsAfterCommit(
                order.getScheduleId(),
                order.getFromStopIndex(),
                order.getToStopIndex()
        );
        return getOrderOrThrow(order.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOrder> listPurchasedOrders(Long userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, userId)
                        .orderByDesc(TicketOrder::getCreatedAt)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketOrder> listMyTickets(Long userId) {
        UserIdentity identity = identityService.findVerifiedIdentityByUserId(userId);
        return orderMapper.selectMyTickets(
                userId,
                identity == null ? null : identity.getRealName(),
                identity == null ? null : identity.getIdType(),
                identity == null ? null : identity.getIdNo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketOrder getVisibleOrder(Long userId, Long orderId) {
        UserIdentity identity = identityService.findVerifiedIdentityByUserId(userId);
        TicketOrder order = orderMapper.selectVisibleOrder(
                orderId,
                userId,
                identity == null ? null : identity.getRealName(),
                identity == null ? null : identity.getIdType(),
                identity == null ? null : identity.getIdNo()
        );
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder cancelOrder(Long userId, Long orderId) {
        TicketOrder order = getBuyerOrderForUpdate(userId, orderId);
        if (OrderStatus.CLOSED.name().equals(order.getStatus())) {
            return order;
        }
        requireStatus(order, OrderStatus.PENDING_PAYMENT, "只有待支付订单可以取消");
        restoreInventory(order);
        changeStatus(order, OrderStatus.CLOSED, "用户取消订单");
        insertOrderOutbox(order, OrderEventType.ORDER_CANCELLED, LocalDateTime.now());
        evictOrderSearchCache(order);
        return getOrderOrThrow(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder payOrder(Long userId, Long orderId) {
        TicketOrder order = getBuyerOrderForUpdate(userId, orderId);
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            return order;
        }
        requireStatus(order, OrderStatus.PENDING_PAYMENT, "只有待支付订单可以支付");
        if (!order.getExpireAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "订单已超过支付期限，请等待系统关单");
        }
        order.setPaidAt(LocalDateTime.now());
        changeStatus(order, OrderStatus.PAID, "模拟支付成功");
        insertOrderOutbox(order, OrderEventType.ORDER_PAID, LocalDateTime.now());
        return getOrderOrThrow(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketOrder refundOrder(Long userId, Long orderId) {
        TicketOrder order = getBuyerOrderForUpdate(userId, orderId);
        if (OrderStatus.REFUNDED.name().equals(order.getStatus())) {
            return order;
        }
        requireStatus(order, OrderStatus.PAID, "只有已支付订单可以退票");
        validateRefundAllowed(order); //校验
        restoreInventory(order);
        order.setRefundedAt(LocalDateTime.now()); //设置退票时间
        changeStatus(order, OrderStatus.REFUNDED, "模拟退票成功");
        insertOrderOutbox(order, OrderEventType.ORDER_REFUNDED, LocalDateTime.now());
        evictOrderSearchCache(order);
        return getOrderOrThrow(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExpiredOrder(Long orderId) {
        TicketOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            return;
        }
        if (order.getExpireAt().isAfter(LocalDateTime.now())) {
            return;
        }
        restoreInventory(order);
        changeStatus(order, OrderStatus.CLOSED, "支付超时自动关单");
        insertOrderOutbox(order, OrderEventType.ORDER_TIMEOUT_CLOSED, LocalDateTime.now());
        evictOrderSearchCache(order);
    }

    private TicketOrder buildOrder(
            Long userId,
            Passenger passenger,
            UserIdentity passengerIdentity,
            TicketOrderCreateRequest request,
            String seatTypeCode,
            OrderCreateContext context,
            LocalDateTime now
    ) {
        long orderId = IdWorker.getId();
        TicketOrder order = new TicketOrder();
        order.setId(orderId);
        order.setOrderNo("RT" + orderId);
        order.setUserId(userId);
        order.setPassengerId(passenger.getId());
        order.setPassengerUserId(passengerIdentity == null ? null : passengerIdentity.getUserId());
        order.setPassengerName(passenger.getName());
        order.setPassengerIdType(passenger.getIdType());
        order.setPassengerIdNo(passenger.getIdNo());
        order.setPassengerPhone(passenger.getPhone());
        order.setScheduleId(request.scheduleId());
        order.setSeatTypeCode(seatTypeCode);
        order.setFromStopIndex(request.fromStopIndex());
        order.setToStopIndex(request.toStopIndex());
        order.setAmount(context.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setRequestId(request.requestId().trim());
        order.setExpireAt(now.plusMinutes(paymentTimeoutMinutes));
        return order;
    }

    private void deductInventory(TicketOrder order) {
        int expectedRows = order.getToStopIndex() - order.getFromStopIndex();
        int affectedRows = inventoryMapper.deductAvailableSegments(
                order.getScheduleId(),
                order.getSeatTypeCode(),
                order.getFromStopIndex(),
                order.getToStopIndex()
        );
        if (affectedRows != expectedRows) {
            throw new BusinessException(409, "余票不足或区段库存数据不完整");
        }
    }

    private void restoreInventory(TicketOrder order) {
        int expectedRows = order.getToStopIndex() - order.getFromStopIndex();
        int affectedRows = inventoryMapper.restoreAvailableSegments(
                order.getScheduleId(),
                order.getSeatTypeCode(),
                order.getFromStopIndex(),
                order.getToStopIndex()
        );
        if (affectedRows != expectedRows) {
            throw new BusinessException(500, "恢复区段库存失败，请检查库存数据");
        }
    }

    private void changeStatus(TicketOrder order, OrderStatus targetStatus, String reason) {
        String previousStatus = order.getStatus();
        order.setStatus(targetStatus.name());
        if (orderMapper.updateById(order) == 0) {
            throw new BusinessException(409, "订单状态已发生变化，请刷新后重试");
        }
        insertStatusLog(order.getId(), previousStatus, targetStatus, reason);
    }

    private void insertStatusLog(
            Long orderId,
            String fromStatus,
            OrderStatus toStatus,
            String reason
    ) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus.name());
        log.setReason(reason);
        if (statusLogMapper.insert(log) == 0) {
            throw new BusinessException(500, "记录订单状态失败");
        }
    }

    private void insertOrderOutbox(
            TicketOrder order,
            OrderEventType eventType,
            LocalDateTime now
    ) {
        MessageOutbox outbox = new MessageOutbox();
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(order.getId());
        outbox.setEventType(eventType.name());
        outbox.setPayload(eventType == OrderEventType.ORDER_TIMEOUT
                ? order.getId().toString()
                : serializeNotificationMessage(order, now));
        outbox.setStatus(OutboxStatus.NEW.name());
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(now);
        if (outboxMapper.insert(outbox) == 0) {
            throw new BusinessException(500, "创建订单消息失败");
        }
        eventPublisher.publishEvent(new OrderOutboxCreatedEvent(outbox.getId()));
    }

    private String serializeNotificationMessage(TicketOrder order, LocalDateTime occurredAt) {
        try {
            return objectMapper.writeValueAsString(
                    OrderNotificationMessage.from(order, occurredAt)
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "序列化订单通知消息失败");
        }
    }

    private TicketOrder getBuyerOrderForUpdate(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private TicketOrder getOrderOrThrow(Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private TicketOrder findByRequestId(Long userId, String requestId) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, userId)
                        .eq(TicketOrder::getRequestId, requestId)
        );
    }

    private void validateOrderCreateContext(OrderCreateContext context) {
        if (context == null || !Integer.valueOf(1).equals(context.getScheduleStatus())) {
            throw new BusinessException(404, "运行计划不存在或已停售");
        }
        if (context.getTravelDate().isBefore(LocalDate.now())) {
            throw new BusinessException(409, "不能购买已过期运行计划的车票");
        }
        if (!Integer.valueOf(1).equals(context.getTrainStatus())) {
            throw new BusinessException(404, "车次不存在或已停运");
        }
        if (context.getFromStopId() == null || context.getToStopId() == null) {
            throw new BusinessException(400, "出发站或到达站不属于该车次");
        }
        if (!Integer.valueOf(1).equals(context.getFromStationStatus())
                || !Integer.valueOf(1).equals(context.getToStationStatus())) {
            throw new BusinessException(409, "出发站或到达站已停用");
        }
        if (context.getDepartureOffsetMinutes() == null) {
            throw new BusinessException(500, "出发站发车时间配置不完整");
        }
        if (context.getFareId() == null || context.getPrice() == null) {
            throw new BusinessException(404, "当前区间和席别尚未配置票价");
        }
        LocalDateTime departureTime = context.getTravelDate()
                .atStartOfDay()
                .plusMinutes(context.getDepartureOffsetMinutes());
        if (!departureTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "列车已从所选出发站发车，无法购票");
        }
    }

    private void validateRefundAllowed(TicketOrder order) {
        TrainSchedule schedule = scheduleMapper.selectById(order.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(500, "订单关联的运行计划不存在");
        }
        TrainStop departureStop = trainStopMapper.selectOne(
                new LambdaQueryWrapper<TrainStop>()
                        .eq(TrainStop::getTrainId, schedule.getTrainId())
                        .eq(TrainStop::getStopIndex, order.getFromStopIndex())
        );
        if (departureStop == null || departureStop.getDepartureOffsetMinutes() == null) {
            throw new BusinessException(500, "订单关联的发车时间配置不完整");
        }
        LocalDateTime refundDeadline = schedule.getTravelDate()
                .atStartOfDay()
                .plusMinutes(departureStop.getDepartureOffsetMinutes())
                .minusMinutes(30);
        if (!LocalDateTime.now().isBefore(refundDeadline)) {
            throw new BusinessException(409, "距离发车不足 30 分钟，不能退票");
        }
    }

    private void evictOrderSearchCache(TicketOrder order) {
        searchCacheService.evictSegmentsAfterCommit(
                order.getScheduleId(),
                order.getFromStopIndex(),
                order.getToStopIndex()
        );
    }

    private void validateRouteAndSeat(
            Integer fromStopIndex,
            Integer toStopIndex,
            String seatTypeCode
    ) {
        if (fromStopIndex >= toStopIndex) {
            throw new BusinessException(400, "出发站顺序必须小于到达站顺序");
        }
        if (!SeatType.contains(seatTypeCode)) {
            throw new BusinessException(400, "不支持的席别编码");
        }
    }

    private void requireStatus(TicketOrder order, OrderStatus expected, String message) {
        if (!expected.name().equals(order.getStatus())) {
            throw new BusinessException(409, message);
        }
    }
}
