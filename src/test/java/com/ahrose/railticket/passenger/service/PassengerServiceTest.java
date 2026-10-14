package com.ahrose.railticket.passenger.service;

import com.ahrose.railticket.bean.Passenger;
import com.ahrose.railticket.bean.SysUser;
import com.ahrose.railticket.bean.request.PassengerCreateRequest;
import com.ahrose.railticket.bean.request.PassengerPhoneUpdateRequest;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class PassengerServiceTest {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private UserMapper userMapper;

    private SysUser user;
    private String suffix;

    /** 每个测试执行前创建独立用户，测试结束后由事务自动回滚。 */
    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user = new SysUser();
        user.setUsername("passenger_" + suffix);
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    /** 验证新增乘车人时会标准化证件信息，并能够在当前用户列表中查询到。 */
    @Test
    void createPassenger_shouldSaveAndReturnPassenger() {
        Passenger created = createPassenger("张三", "id_card", "id" + suffix, "13800138000");
        List<Passenger> passengers = passengerService.listPassengers(user.getId());

        assertNotNull(created.getId());
        assertEquals("ID_CARD", created.getIdType());
        assertEquals(("id" + suffix).toUpperCase(), created.getIdNo());
        assertEquals(1, passengers.size());
        assertEquals(created.getId(), passengers.getFirst().getId());
        printPassenger("新增乘车人测试", created);
    }

    /** 验证同一用户不能重复添加相同证件的常用乘车人。 */
    @Test
    void createPassenger_whenDocumentRepeated_shouldThrowConflict() {
        createPassenger("张三", "ID_CARD", "ID" + suffix, "13800138000");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> createPassenger("张三", "ID_CARD", "ID" + suffix, "13900139000")
        );

        assertEquals(409, exception.getCode());
        assertEquals("该证件号码已添加为常用乘车人", exception.getMessage());
        printException("重复添加乘车人测试", exception);
    }

    /** 验证乘车人创建后只允许修改联系电话，姓名和证件信息保持不变。 */
    @Test
    void updatePassengerPhone_shouldOnlyChangePhone() {
        Passenger created = createPassenger("李四", "ID_CARD", "ID" + suffix, "13800138000");

        Passenger updated = passengerService.updatePassengerPhone(
                user.getId(),
                created.getId(),
                new PassengerPhoneUpdateRequest("13900139000")
        );

        assertEquals("13900139000", updated.getPhone());
        assertEquals(created.getName(), updated.getName());
        assertEquals(created.getIdNo(), updated.getIdNo());
        printPassenger("修改乘车人手机号测试", updated);
    }

    /** 验证用户不能查询其他账号维护的常用乘车人。 */
    @Test
    void getPassenger_whenNotOwned_shouldThrowNotFound() {
        Passenger created = createPassenger("王五", "ID_CARD", "ID" + suffix, null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> passengerService.getPassenger(user.getId() + 1, created.getId())
        );

        assertEquals(404, exception.getCode());
        assertEquals("乘车人不存在", exception.getMessage());
        printException("乘车人越权查询测试", exception);
    }

    /** 验证删除常用乘车人后，当前用户列表中不再返回该记录。 */
    @Test
    void deletePassenger_shouldRemovePassenger() {
        Passenger created = createPassenger("赵六", "ID_CARD", "ID" + suffix, null);

        passengerService.deletePassenger(user.getId(), created.getId());

        assertEquals(0, passengerService.listPassengers(user.getId()).size());
        System.out.println("========== 删除乘车人测试 ==========");
        System.out.println("测试结果：通过");
    }

    private Passenger createPassenger(
            String name,
            String idType,
            String idNo,
            String phone
    ) {
        return passengerService.createPassenger(
                user.getId(),
                new PassengerCreateRequest(name, idType, idNo, phone)
        );
    }

    private void printPassenger(String testName, Passenger passenger) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "passengerId=%d, name=%s, idType=%s, idNo=%s, phone=%s%n",
                passenger.getId(),
                passenger.getName(),
                passenger.getIdType(),
                passenger.getIdNo(),
                passenger.getPhone()
        );
        System.out.println("测试结果：通过");
    }

    private void printException(String testName, BusinessException exception) {
        System.out.println("========== " + testName + " ==========");
        System.out.println("异常状态码：" + exception.getCode());
        System.out.println("异常信息：" + exception.getMessage());
        System.out.println("测试结果：通过");
    }
}
