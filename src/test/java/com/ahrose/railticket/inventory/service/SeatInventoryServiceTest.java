package com.ahrose.railticket.inventory.service;

import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.SeatInventoryInitializeRequest;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class SeatInventoryServiceTest {

    @Autowired
    private SeatInventoryService seatInventoryService;

    @Autowired
    private SeatInventoryMapper seatInventoryMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    private Train train;
    private TrainSchedule trainSchedule;

    /** 每个测试执行前创建一辆车、四个连续经停站和一个运行计划，测试结束后自动回滚。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        train = new Train();
        train.setTrainNo("INV-" + suffix);
        train.setName("库存测试车次");
        train.setStatus(1);
        trainMapper.insert(train);

        Station firstStation = createStation("A" + suffix, "库存测试一站");
        Station secondStation = createStation("B" + suffix, "库存测试二站");
        Station thirdStation = createStation("C" + suffix, "库存测试三站");
        Station fourthStation = createStation("D" + suffix, "库存测试四站");

        insertTrainStop(firstStation.getId(), 0, null, 480);
        insertTrainStop(secondStation.getId(), 1, 540, 545);
        insertTrainStop(thirdStation.getId(), 2, 600, 605);
        insertTrainStop(fourthStation.getId(), 3, 660, null);

        trainSchedule = new TrainSchedule();
        trainSchedule.setTrainId(train.getId());
        trainSchedule.setTravelDate(LocalDate.now().plusDays(1));
        trainSchedule.setStatus(1);
        trainScheduleMapper.insert(trainSchedule);
    }

    /** 验证库存查询可以按席别筛选，并按照区段顺序升序返回。 */
    @Test
    void listSeatInventories_shouldFilterBySeatTypeAndOrderBySegmentIndex() {
        insertInventory(SeatType.SECOND_CLASS.getCode(), 2, 600, 580);
        insertInventory(SeatType.FIRST_CLASS.getCode(), 0, 100, 100);
        insertInventory(SeatType.SECOND_CLASS.getCode(), 0, 600, 590);
        insertInventory(SeatType.SECOND_CLASS.getCode(), 1, 600, 585);

        List<SeatInventory> result = seatInventoryService.listSeatInventories(
                train.getId(),
                trainSchedule.getId(),
                SeatType.SECOND_CLASS.getCode()
        );

        printInventories("按席别查询库存测试", result);
        assertEquals(3, result.size());
        assertEquals(List.of(0, 1, 2), result.stream().map(SeatInventory::getSegmentIndex).toList());
        assertTrue(result.stream().allMatch(
                inventory -> SeatType.SECOND_CLASS.getCode().equals(inventory.getSeatTypeCode())
        ));
        System.out.println("测试结果：通过");
    }

    /** 验证初始化一个席别时，会按照四个经停站创建三条相邻区段库存。 */
    @Test
    void initializeSeatInventory_shouldCreateInventoryForEverySegment() {
        List<SeatInventory> result = seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 600)
        );

        printInventories("初始化库存测试", result);
        assertEquals(3, result.size());
        assertEquals(List.of(0, 1, 2), result.stream().map(SeatInventory::getSegmentIndex).toList());
        for (SeatInventory inventory : result) {
            assertNotNull(inventory.getId());
            assertEquals(trainSchedule.getId(), inventory.getScheduleId());
            assertEquals(600, inventory.getTotal());
            assertEquals(600, inventory.getAvailable());
        }
        System.out.println("测试结果：通过");
    }

    /** 验证同一运行计划的同一席别不能重复初始化库存。 */
    @Test
    void initializeSeatInventory_whenSameSeatTypeExists_shouldThrowConflict() {
        SeatInventoryInitializeRequest request = new SeatInventoryInitializeRequest(
                SeatType.SECOND_CLASS.getCode(),
                600
        );
        seatInventoryService.initializeSeatInventory(train.getId(), trainSchedule.getId(), request);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> seatInventoryService.initializeSeatInventory(
                        train.getId(),
                        trainSchedule.getId(),
                        request
                )
        );

        printException("重复初始化库存测试", exception);
        assertEquals(409, exception.getCode());
        assertEquals("该运行计划的此席别库存已初始化", exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证调整总库存时会保留每个区段已经售出的数量。 */
    @Test
    void updateSeatInventoryTotal_shouldKeepSoldCountForEverySegment() {
        List<SeatInventory> initialized = seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 600)
        );
        updateAvailable(initialized.get(0), 580);
        updateAvailable(initialized.get(1), 570);
        updateAvailable(initialized.get(2), 590);

        List<SeatInventory> result = seatInventoryService.updateSeatInventoryTotal(
                train.getId(),
                trainSchedule.getId(),
                SeatType.SECOND_CLASS.getCode(),
                700
        );

        printInventories("调整总库存测试", result);
        assertEquals(List.of(700, 700, 700), result.stream().map(SeatInventory::getTotal).toList());
        assertEquals(List.of(680, 670, 690), result.stream().map(SeatInventory::getAvailable).toList());
        System.out.println("测试结果：通过");
    }

    /** 验证新总库存小于任一区段已售数量时会拒绝调整。 */
    @Test
    void updateSeatInventoryTotal_whenNewTotalLessThanSold_shouldThrowConflict() {
        List<SeatInventory> initialized = seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 100)
        );
        updateAvailable(initialized.get(1), 60);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> seatInventoryService.updateSeatInventoryTotal(
                        train.getId(),
                        trainSchedule.getId(),
                        SeatType.SECOND_CLASS.getCode(),
                        30
                )
        );

        printException("总库存小于已售数量测试", exception);
        assertEquals(409, exception.getCode());
        assertTrue(exception.getMessage().contains("已售 40 张"));
        System.out.println("测试结果：通过");
    }

    /** 验证运行计划停售且没有已售库存时，可以删除该席别全部区段库存。 */
    @Test
    void deleteSeatInventory_whenScheduleStoppedAndNoTicketsSold_shouldDeleteAllSegments() {
        seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 600)
        );
        updateScheduleStatus(0);

        seatInventoryService.deleteSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                SeatType.SECOND_CLASS.getCode()
        );

        long count = countInventories(SeatType.SECOND_CLASS.getCode());
        System.out.println("========== 删除库存测试 ==========");
        System.out.println("删除后库存数量：" + count);
        assertEquals(0L, count);
        System.out.println("测试结果：通过");
    }

    /** 验证运行计划仍处于可售状态时，不允许删除库存。 */
    @Test
    void deleteSeatInventory_whenScheduleEnabled_shouldThrowConflict() {
        seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 600)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> seatInventoryService.deleteSeatInventory(
                        train.getId(),
                        trainSchedule.getId(),
                        SeatType.SECOND_CLASS.getCode()
                )
        );

        printException("可售运行计划删除库存测试", exception);
        assertEquals(409, exception.getCode());
        assertEquals("请先将运行计划设置为停运或停售", exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证库存已经产生售票占用时，即使运行计划停售也不允许删除库存。 */
    @Test
    void deleteSeatInventory_whenTicketsSold_shouldThrowConflict() {
        List<SeatInventory> initialized = seatInventoryService.initializeSeatInventory(
                train.getId(),
                trainSchedule.getId(),
                new SeatInventoryInitializeRequest(SeatType.SECOND_CLASS.getCode(), 600)
        );
        updateAvailable(initialized.get(0), 599);
        updateScheduleStatus(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> seatInventoryService.deleteSeatInventory(
                        train.getId(),
                        trainSchedule.getId(),
                        SeatType.SECOND_CLASS.getCode()
                )
        );

        printException("已有售票记录删除库存测试", exception);
        assertEquals(409, exception.getCode());
        assertEquals("该席别已经产生售票记录，不能删除库存", exception.getMessage());
        System.out.println("测试结果：通过");
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

    private void insertTrainStop(Long stationId,
                                 Integer stopIndex,
                                 Integer arrivalOffsetMinutes,
                                 Integer departureOffsetMinutes) {
        TrainStop trainStop = new TrainStop();
        trainStop.setTrainId(train.getId());
        trainStop.setStationId(stationId);
        trainStop.setStopIndex(stopIndex);
        trainStop.setArrivalOffsetMinutes(arrivalOffsetMinutes);
        trainStop.setDepartureOffsetMinutes(departureOffsetMinutes);
        trainStopMapper.insert(trainStop);
    }

    private SeatInventory insertInventory(String seatTypeCode,
                                          Integer segmentIndex,
                                          Integer total,
                                          Integer available) {
        SeatInventory inventory = new SeatInventory();
        inventory.setScheduleId(trainSchedule.getId());
        inventory.setSeatTypeCode(seatTypeCode);
        inventory.setSegmentIndex(segmentIndex);
        inventory.setTotal(total);
        inventory.setAvailable(available);
        seatInventoryMapper.insert(inventory);
        return inventory;
    }

    private void updateAvailable(SeatInventory inventory, Integer available) {
        inventory.setAvailable(available);
        seatInventoryMapper.updateById(inventory);
    }

    private void updateScheduleStatus(Integer status) {
        trainSchedule.setStatus(status);
        trainScheduleMapper.updateById(trainSchedule);
    }

    private long countInventories(String seatTypeCode) {
        return seatInventoryMapper.selectCount(
                new LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, trainSchedule.getId())
                        .eq(SeatInventory::getSeatTypeCode, seatTypeCode)
        );
    }

    private void printInventories(String testName, List<SeatInventory> inventories) {
        System.out.println();
        System.out.println("========== " + testName + " ==========");
        System.out.println("测试车次 ID：" + train.getId());
        System.out.println("运行计划 ID：" + trainSchedule.getId());
        inventories.forEach(inventory -> System.out.printf(
                "inventoryId=%d, seatType=%s, segment=%d, total=%d, available=%d%n",
                inventory.getId(),
                inventory.getSeatTypeCode(),
                inventory.getSegmentIndex(),
                inventory.getTotal(),
                inventory.getAvailable()
        ));
    }

    private void printException(String testName, BusinessException exception) {
        System.out.println();
        System.out.println("========== " + testName + " ==========");
        System.out.println("异常状态码：" + exception.getCode());
        System.out.println("异常信息：" + exception.getMessage());
    }
}
