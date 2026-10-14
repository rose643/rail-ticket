package com.ahrose.railticket.order.service;

import com.ahrose.railticket.bean.MessageOutbox;
import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.TicketOrderCreateRequest;
import com.ahrose.railticket.common.enums.OrderStatus;
import com.ahrose.railticket.common.enums.OutboxStatus;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.order.mapper.MessageOutboxMapper;
import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import com.ahrose.railticket.order.event.OrderNotificationMessage;
import com.ahrose.railticket.passenger.mapper.PassengerMapper;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.ahrose.railticket.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class TicketOrderServiceTest {

    @Autowired
    private TicketOrderService orderService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PassengerMapper passengerMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainScheduleMapper scheduleMapper;

    @Autowired
    private TrainFareMapper fareMapper;

    @Autowired
    private SeatInventoryMapper inventoryMapper;

    @Autowired
    private MessageOutboxMapper outboxMapper;

    @Autowired
    private TicketOrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private SysUser buyer;
    private Passenger passenger;
    private Train train;
    private TrainSchedule schedule;
    private final Set<String> requestIds = new HashSet<>();

    /** 每个测试执行前准备用户、乘车人、车次、运行计划、票价及两个区段库存。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        buyer = new SysUser();
        buyer.setUsername("order_buyer_" + suffix.toLowerCase());
        buyer.setPasswordHash("test-password-hash");
        buyer.setRole("USER");
        buyer.setStatus(1);
        userMapper.insert(buyer);

        passenger = new Passenger();
        passenger.setUserId(buyer.getId());
        passenger.setName("订单测试乘车人");
        passenger.setIdType("ID_CARD");
        passenger.setIdNo("ORDER-ID-" + suffix);
        passenger.setPhone("13800138000");
        passengerMapper.insert(passenger);

        train = new Train();
        train.setTrainNo("ORDER-" + suffix);
        train.setName("订单测试车次");
        train.setStatus(1);
        trainMapper.insert(train);

        Station first = createStation("A" + suffix, "订单测试始发站");
        Station middle = createStation("B" + suffix, "订单测试中间站");
        Station last = createStation("C" + suffix, "订单测试终点站");
        insertStop(first.getId(), 0, null, 480);
        insertStop(middle.getId(), 1, 540, 545);
        insertStop(last.getId(), 2, 600, null);

        schedule = new TrainSchedule();
        schedule.setTrainId(train.getId());
        schedule.setTravelDate(LocalDate.now().plusDays(2));
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);

        TrainFare fare = new TrainFare();
        fare.setTrainId(train.getId());
        fare.setSeatTypeCode(SeatType.SECOND_CLASS.getCode());
        fare.setFromStopIndex(0);
        fare.setToStopIndex(2);
        fare.setPrice(new BigDecimal("200.00"));
        fareMapper.insert(fare);

        insertInventory(0, 2);
        insertInventory(1, 2);
    }

    /** 每个测试结束后删除下单防重复 Redis 标记，避免测试数据残留。 */
    @AfterEach
    void clearRequestGuards() {
        requestIds.forEach(requestId -> redisTemplate.delete(
                "order:create:" + buyer.getId() + ":" + requestId
        ));
    }

    /** 验证创建订单会保存服务端价格、扣减全部乘车区段库存并写入 Outbox。 */
    @Test
    void createOrder_shouldDeductSegmentsAndCreateOutbox() {
        TicketOrder order = createOrder(newRequestId());

        assertNotNull(order.getId());
        assertEquals(new BigDecimal("200.00"), order.getAmount());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), order.getStatus());
        assertEquals(List.of(1, 1), listAvailable());

        MessageOutbox outbox = findOutbox(order.getId(), "ORDER_TIMEOUT");
        assertNotNull(outbox);
        assertEquals(OutboxStatus.NEW.name(), outbox.getStatus());
        assertEquals(order.getId().toString(), outbox.getPayload());
        printOrder("创建订单与扣减库存测试", order);
    }

    /** 验证下单通知 Outbox 保存了消费者所需的完整 JSON 快照，而不是只有订单 ID。 */
    @Test
    void createOrder_shouldWriteSelfContainedNotificationMessage() throws JsonProcessingException {
        TicketOrder order = createOrder(newRequestId());
        MessageOutbox outbox = findOutbox(order.getId(), "ORDER_CREATED");

        assertNotNull(outbox);
        OrderNotificationMessage message = objectMapper.readValue(
                outbox.getPayload(),
                OrderNotificationMessage.class
        );
        assertEquals(order.getId(), message.orderId());
        assertEquals(order.getOrderNo(), message.orderNo());
        assertEquals(buyer.getId(), message.buyerUserId());
        assertEquals(order.getPassengerUserId(), message.passengerUserId());
        assertEquals(order.getAmount(), message.amount());
        assertEquals(order.getExpireAt().withNano(0), message.expireAt().withNano(0));
        assertNotNull(message.occurredAt());
        System.out.println("通知消息JSON=" + outbox.getPayload());
    }

    /** 验证同一用户使用相同 requestId 重复下单时返回同一订单且库存只扣减一次。 */
    @Test
    void createOrder_whenRequestRepeated_shouldReturnSameOrder() {
        String requestId = newRequestId();

        TicketOrder first = createOrder(requestId);
        TicketOrder second = createOrder(requestId);

        assertEquals(first.getId(), second.getId());
        assertEquals(List.of(1, 1), listAvailable());
        assertEquals(2L, outboxMapper.selectCount(
                new LambdaQueryWrapper<MessageOutbox>()
                        .eq(MessageOutbox::getAggregateId, first.getId())
        ));
        printOrder("订单幂等测试", second);
    }

    /** 验证已支付订单退票后，全部乘车区段库存恢复且状态变为 REFUNDED。 */
    @Test
    void refundOrder_shouldRestoreAllSegments() {
        TicketOrder created = createOrder(newRequestId());
        TicketOrder paid = orderService.payOrder(buyer.getId(), created.getId());
        TicketOrder refunded = orderService.refundOrder(buyer.getId(), created.getId());

        assertEquals(OrderStatus.PAID.name(), paid.getStatus());
        assertEquals(OrderStatus.REFUNDED.name(), refunded.getStatus());
        assertNotNull(refunded.getRefundedAt());
        assertEquals(List.of(2, 2), listAvailable());
        assertNotNull(findOutbox(created.getId(), "ORDER_PAID"));
        assertNotNull(findOutbox(created.getId(), "ORDER_REFUNDED"));
        printOrder("支付后退票与库存恢复测试", refunded);
    }

    /** 验证主动取消待支付订单后，全部乘车区段库存恢复且状态变为 CLOSED。 */
    @Test
    void cancelOrder_shouldRestoreAllSegments() {
        TicketOrder created = createOrder(newRequestId());
        TicketOrder cancelled = orderService.cancelOrder(buyer.getId(), created.getId());

        assertEquals(OrderStatus.CLOSED.name(), cancelled.getStatus());
        assertEquals(List.of(2, 2), listAvailable());
        assertNotNull(findOutbox(created.getId(), "ORDER_CANCELLED"));
        printOrder("取消订单与库存恢复测试", cancelled);
    }

    /** 验证重复退票不会再次增加区段库存。 */
    @Test
    void refundOrder_whenRepeated_shouldNotRestoreInventoryTwice() {
        TicketOrder created = createOrder(newRequestId());
        orderService.payOrder(buyer.getId(), created.getId());

        TicketOrder first = orderService.refundOrder(buyer.getId(), created.getId());
        TicketOrder second = orderService.refundOrder(buyer.getId(), created.getId());

        assertEquals(first.getId(), second.getId());
        assertEquals(OrderStatus.REFUNDED.name(), second.getStatus());
        assertEquals(List.of(2, 2), listAvailable());
        assertEquals(1L, outboxMapper.selectCount(
                new LambdaQueryWrapper<MessageOutbox>()
                        .eq(MessageOutbox::getAggregateId, created.getId())
                        .eq(MessageOutbox::getEventType, "ORDER_REFUNDED")
        ));
        printOrder("重复退票幂等测试", second);
    }

    /** 验证待支付订单到期后会自动关闭并恢复库存，重复关单不会再次回补。 */
    @Test
    void closeExpiredOrder_shouldCloseOnceAndRestoreAllSegments() {
        TicketOrder created = createOrder(newRequestId());
        created.setExpireAt(LocalDateTime.now().minusSeconds(1));
        orderMapper.updateById(created);

        orderService.closeExpiredOrder(created.getId());
        orderService.closeExpiredOrder(created.getId());

        TicketOrder closed = orderMapper.selectById(created.getId());
        assertEquals(OrderStatus.CLOSED.name(), closed.getStatus());
        assertEquals(List.of(2, 2), listAvailable());
        assertEquals(1L, outboxMapper.selectCount(
                new LambdaQueryWrapper<MessageOutbox>()
                        .eq(MessageOutbox::getAggregateId, created.getId())
                        .eq(MessageOutbox::getEventType, "ORDER_TIMEOUT_CLOSED")
        ));
        printOrder("超时关单幂等与库存恢复测试", closed);
    }

    private TicketOrder createOrder(String requestId) {
        return orderService.createOrder(
                buyer.getId(),
                new TicketOrderCreateRequest(
                        passenger.getId(),
                        schedule.getId(),
                        SeatType.SECOND_CLASS.getCode(),
                        0,
                        2,
                        requestId
                )
        );
    }

    private String newRequestId() {
        String requestId = UUID.randomUUID().toString();
        requestIds.add(requestId);
        return requestId;
    }

    private Station createStation(String code, String name) {
        Station station = new Station();
        station.setCode(code);
        station.setName(name);
        station.setCity("测试城市");
        station.setStatus(1);
        station.setVersion(0);
        stationMapper.insert(station);
        return station;
    }

    private void insertStop(Long stationId,
                            Integer stopIndex,
                            Integer arrival,
                            Integer departure) {
        TrainStop stop = new TrainStop();
        stop.setTrainId(train.getId());
        stop.setStationId(stationId);
        stop.setStopIndex(stopIndex);
        stop.setArrivalOffsetMinutes(arrival);
        stop.setDepartureOffsetMinutes(departure);
        trainStopMapper.insert(stop);
    }

    private void insertInventory(Integer segmentIndex, Integer available) {
        SeatInventory inventory = new SeatInventory();
        inventory.setScheduleId(schedule.getId());
        inventory.setSeatTypeCode(SeatType.SECOND_CLASS.getCode());
        inventory.setSegmentIndex(segmentIndex);
        inventory.setTotal(2);
        inventory.setAvailable(available);
        inventoryMapper.insert(inventory);
    }

    private List<Integer> listAvailable() {
        return inventoryMapper.selectList(
                        new LambdaQueryWrapper<SeatInventory>()
                                .eq(SeatInventory::getScheduleId, schedule.getId())
                                .eq(SeatInventory::getSeatTypeCode, SeatType.SECOND_CLASS.getCode())
                                .orderByAsc(SeatInventory::getSegmentIndex)
                ).stream()
                .map(SeatInventory::getAvailable)
                .toList();
    }

    private MessageOutbox findOutbox(Long orderId, String eventType) {
        return outboxMapper.selectOne(
                new LambdaQueryWrapper<MessageOutbox>()
                        .eq(MessageOutbox::getAggregateType, "TICKET_ORDER")
                        .eq(MessageOutbox::getAggregateId, orderId)
                        .eq(MessageOutbox::getEventType, eventType)
        );
    }

    private void printOrder(String testName, TicketOrder order) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "orderId=%d, orderNo=%s, amount=%s, status=%s, inventory=%s%n",
                order.getId(),
                order.getOrderNo(),
                order.getAmount(),
                order.getStatus(),
                listAvailable()
        );
        System.out.println("测试结果：通过");
    }
}
