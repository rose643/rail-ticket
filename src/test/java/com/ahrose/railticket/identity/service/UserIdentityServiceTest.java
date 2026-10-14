package com.ahrose.railticket.identity.service;

import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.bean.UserIdentity;
import com.ahrose.railticket.bean.request.UserIdentityBindRequest;
import com.ahrose.railticket.common.enums.OrderStatus;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.order.mapper.TicketOrderMapper;
import com.ahrose.railticket.order.service.TicketOrderService;
import com.ahrose.railticket.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserIdentityServiceTest {

    @Autowired
    private UserIdentityService identityService;

    @Autowired
    private TicketOrderService orderService;

    @Autowired
    private TicketOrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    private SysUser passengerUser;
    private SysUser buyerUser;
    private String idNo;

    /** 每个测试执行前创建购票人、实际乘车人和唯一测试证件号。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        passengerUser = createUser("identity_passenger_" + suffix);
        buyerUser = createUser("identity_buyer_" + suffix);
        idNo = ("TEST-ID-" + suffix).toUpperCase();
    }

    /** 验证用户可以绑定实名认证，并能通过用户 ID 和证件信息查询有效实名记录。 */
    @Test
    void bindIdentity_shouldSaveVerifiedIdentity() {
        UserIdentity identity = bindPassengerIdentity();

        assertNotNull(identity.getId());
        assertEquals(passengerUser.getId(), identity.getUserId());
        assertEquals("ID_CARD", identity.getIdType());
        assertEquals(idNo, identity.getIdNo());
        assertEquals(identity.getId(),
                identityService.findVerifiedIdentityByUserId(passengerUser.getId()).getId());
        assertEquals(identity.getId(),
                identityService.findVerifiedIdentity("ID_CARD", idNo).getId());
        printIdentity("绑定实名认证测试", identity);
    }

    /** 验证实名认证成功时会把实名快照匹配的历史订单关联到实际乘车人账号。 */
    @Test
    void bindIdentity_shouldBackfillHistoricalTicketPassengerUserId() {
        TicketOrder historicalOrder = insertHistoricalOrder("张三", idNo);
        assertNull(historicalOrder.getPassengerUserId());

        bindPassengerIdentity();

        TicketOrder updated = orderMapper.selectById(historicalOrder.getId());
        assertEquals(passengerUser.getId(), updated.getPassengerUserId());
        assertEquals(historicalOrder.getId(), orderService.listMyTickets(passengerUser.getId()).getFirst().getId());
        printOrder("历史车票回填测试", updated);
    }

    /** 验证实名认证姓名与订单快照不一致时，不会错误关联历史订单。 */
    @Test
    void bindIdentity_whenNameMismatch_shouldNotBackfillHistoricalTicket() {
        TicketOrder historicalOrder = insertHistoricalOrder("其他姓名", idNo);

        bindPassengerIdentity();

        TicketOrder unchanged = orderMapper.selectById(historicalOrder.getId());
        assertNull(unchanged.getPassengerUserId());
        assertEquals(0, orderService.listMyTickets(passengerUser.getId()).size());
        printOrder("实名姓名不一致测试", unchanged);
    }

    /** 验证回填结束后新出现的空关联订单仍能通过实名快照兜底查询。 */
    @Test
    void listMyTickets_shouldFindUnlinkedTicketByIdentitySnapshot() {
        bindPassengerIdentity();
        TicketOrder lateOrder = insertHistoricalOrder("张三", idNo);

        List<TicketOrder> tickets = orderService.listMyTickets(passengerUser.getId());

        assertEquals(1, tickets.size());
        assertEquals(lateOrder.getId(), tickets.getFirst().getId());
        assertNull(orderMapper.selectById(lateOrder.getId()).getPassengerUserId());
        printOrder("实名快照兜底查询测试", tickets.getFirst());
    }

    /** 验证同一证件不能绑定到两个不同系统账号。 */
    @Test
    void bindIdentity_whenDocumentAlreadyBound_shouldThrowConflict() {
        bindPassengerIdentity();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> identityService.bindIdentity(
                        buyerUser.getId(),
                        new UserIdentityBindRequest("张三", "ID_CARD", idNo)
                )
        );

        assertEquals(409, exception.getCode());
        assertEquals("该证件已经绑定其他账号", exception.getMessage());
        System.out.println("========== 实名证件重复绑定测试 ==========");
        System.out.println("异常信息：" + exception.getMessage());
        System.out.println("测试结果：通过");
    }

    private UserIdentity bindPassengerIdentity() {
        return identityService.bindIdentity(
                passengerUser.getId(),
                new UserIdentityBindRequest("张三", "id_card", idNo.toLowerCase())
        );
    }

    private SysUser createUser(String username) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private TicketOrder insertHistoricalOrder(String passengerName, String passengerIdNo) {
        long orderId = IdWorker.getId();
        TicketOrder order = new TicketOrder();
        order.setId(orderId);
        order.setOrderNo("RT" + orderId);
        order.setUserId(buyerUser.getId());
        order.setPassengerId(IdWorker.getId());
        order.setPassengerUserId(null);
        order.setPassengerName(passengerName);
        order.setPassengerIdType("ID_CARD");
        order.setPassengerIdNo(passengerIdNo);
        order.setPassengerPhone("13800138000");
        order.setScheduleId(IdWorker.getId());
        order.setSeatTypeCode(SeatType.SECOND_CLASS.getCode());
        order.setFromStopIndex(0);
        order.setToStopIndex(1);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.PAID.name());
        order.setRequestId(UUID.randomUUID().toString());
        order.setExpireAt(LocalDateTime.now().plusMinutes(15));
        order.setPaidAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    private void printIdentity(String testName, UserIdentity identity) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "identityId=%d, userId=%d, name=%s, idType=%s, idNo=%s%n",
                identity.getId(),
                identity.getUserId(),
                identity.getRealName(),
                identity.getIdType(),
                identity.getIdNo()
        );
        System.out.println("测试结果：通过");
    }

    private void printOrder(String testName, TicketOrder order) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "orderId=%d, passengerUserId=%s, name=%s, idNo=%s%n",
                order.getId(),
                order.getPassengerUserId(),
                order.getPassengerName(),
                order.getPassengerIdNo()
        );
        System.out.println("测试结果：通过");
    }
}
