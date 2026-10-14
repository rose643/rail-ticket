package com.ahrose.railticket.train.schedule.service;

import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.request.TrainScheduleCreateRequest;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TrainScheduleServiceTest {

    @Autowired
    private TrainScheduleService trainScheduleService;

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    @Autowired
    private TrainMapper trainMapper;

    private Train train;

    /** 每个测试执行前创建一个独立车次，测试结束后由事务自动回滚。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        train = new Train();
        train.setTrainNo("TEST-" + suffix);
        train.setName("运行计划测试车次");
        train.setStatus(1);
        trainMapper.insert(train);
    }

    /** 验证按照日期范围查询运行计划，并按照始发日期升序返回。 */
    @Test
    void listTrainSchedules_shouldFilterByDateRangeAndOrderByDate() {
        insertSchedule(LocalDate.of(2026, 10, 10), 1);
        insertSchedule(LocalDate.of(2026, 10, 1), 1);
        insertSchedule(LocalDate.of(2026, 10, 5), 0);

        List<TrainSchedule> result = trainScheduleService.listTrainSchedules(
                train.getId(),
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 10)
        );

        printSchedules("日期范围查询测试", result);
        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 10, 5), result.get(0).getTravelDate());
        assertEquals(LocalDate.of(2026, 10, 10), result.get(1).getTravelDate());
        System.out.println("测试结果：通过");
    }

    /** 验证可以根据车次 ID 和运行计划 ID 查询指定运行计划。 */
    @Test
    void getTrainSchedule_shouldReturnSpecifiedSchedule() {
        TrainSchedule saved = insertSchedule(LocalDate.of(2026, 10, 1), 1);

        TrainSchedule result = trainScheduleService.getTrainSchedule(
                train.getId(),
                saved.getId()
        );

        printSchedule("单条运行计划查询测试", result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(train.getId(), result.getTrainId());
        assertEquals(LocalDate.of(2026, 10, 1), result.getTravelDate());
        assertEquals(1, result.getStatus());
        System.out.println("测试结果：通过");
    }

    /** 验证新增运行计划时生成雪花 ID，并将状态默认设置为可售。 */
    @Test
    void createTrainSchedule_shouldCreateEnabledSchedule() {
        LocalDate travelDate = LocalDate.of(2026, 10, 1);

        TrainSchedule created = trainScheduleService.createTrainSchedule(
                train.getId(),
                new TrainScheduleCreateRequest(travelDate)
        );

        TrainSchedule saved = trainScheduleMapper.selectById(created.getId());
        printSchedule("新增运行计划测试", saved);
        assertNotNull(created.getId());
        assertNotNull(saved);
        assertEquals(train.getId(), saved.getTrainId());
        assertEquals(travelDate, saved.getTravelDate());
        assertEquals(1, saved.getStatus());
        System.out.println("测试结果：通过");
    }

    /** 验证同一车次在同一天不能重复创建运行计划。 */
    @Test
    void createTrainSchedule_whenDateAlreadyExists_shouldThrowConflict() {
        LocalDate travelDate = LocalDate.of(2026, 10, 1);
        insertSchedule(travelDate, 1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainScheduleService.createTrainSchedule(
                        train.getId(),
                        new TrainScheduleCreateRequest(travelDate)
                )
        );

        System.out.println("========== 重复运行计划测试 ==========");
        System.out.println("异常状态码：" + exception.getCode());
        System.out.println("异常信息：" + exception.getMessage());
        assertEquals(409, exception.getCode());
        assertEquals("该车次在指定日期已存在运行计划", exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证可以将指定运行计划从可售状态修改为停运或停售状态。 */
    @Test
    void updateTrainScheduleStatus_shouldUpdateStatus() {
        TrainSchedule saved = insertSchedule(LocalDate.of(2026, 10, 1), 1);

        TrainSchedule updated = trainScheduleService.updateTrainScheduleStatus(
                train.getId(),
                saved.getId(),
                0
        );

        TrainSchedule result = trainScheduleMapper.selectById(saved.getId());
        printSchedule("修改运行计划状态测试", result);
        assertEquals(0, updated.getStatus());
        assertEquals(0, result.getStatus());
        System.out.println("测试结果：通过");
    }

    /** 验证删除运行计划后，数据库中不再存在该记录。 */
    @Test
    void deleteTrainSchedule_shouldDeleteSchedule() {
        TrainSchedule saved = insertSchedule(LocalDate.of(2026, 10, 1), 1);

        trainScheduleService.deleteTrainSchedule(train.getId(), saved.getId());

        TrainSchedule result = trainScheduleMapper.selectById(saved.getId());
        System.out.println("========== 删除运行计划测试 ==========");
        System.out.println("已删除运行计划 ID：" + saved.getId());
        assertNull(result);
        System.out.println("测试结果：通过");
    }

    private TrainSchedule insertSchedule(LocalDate travelDate, Integer status) {
        TrainSchedule trainSchedule = new TrainSchedule();
        trainSchedule.setTrainId(train.getId());
        trainSchedule.setTravelDate(travelDate);
        trainSchedule.setStatus(status);
        trainScheduleMapper.insert(trainSchedule);
        return trainSchedule;
    }

    private void printSchedules(String testName, List<TrainSchedule> schedules) {
        System.out.println();
        System.out.println("========== " + testName + " ==========");
        System.out.println("测试车次 ID：" + train.getId());
        schedules.forEach(schedule -> System.out.printf(
                "scheduleId=%d, travelDate=%s, status=%d%n",
                schedule.getId(),
                schedule.getTravelDate(),
                schedule.getStatus()
        ));
    }

    private void printSchedule(String testName, TrainSchedule schedule) {
        printSchedules(testName, List.of(schedule));
    }
}
