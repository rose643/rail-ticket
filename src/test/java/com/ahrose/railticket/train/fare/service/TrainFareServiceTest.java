package com.ahrose.railticket.train.fare.service;

import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.TrainFareCreateRequest;
import com.ahrose.railticket.bean.request.TrainFareUpdateRequest;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TrainFareServiceTest {

    @Autowired
    private TrainFareService trainFareService;

    @Autowired
    private TrainFareMapper trainFareMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainStopMapper trainStopMapper;

    private Train train;

    /** 每个测试执行前创建一个车次和四个连续经停站，测试结束后由事务自动回滚。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        train = new Train();
        train.setTrainNo("TEST-" + suffix);
        train.setName("票价测试车次");
        train.setStatus(1);
        trainMapper.insert(train);

        Station firstStation = createStation("A" + suffix, "票价测试一站");
        Station secondStation = createStation("B" + suffix, "票价测试二站");
        Station thirdStation = createStation("C" + suffix, "票价测试三站");
        Station fourthStation = createStation("D" + suffix, "票价测试四站");

        insertTrainStop(firstStation.getId(), 0, null, 0);
        insertTrainStop(secondStation.getId(), 1, 60, 65);
        insertTrainStop(thirdStation.getId(), 2, 120, 125);
        insertTrainStop(fourthStation.getId(), 3, 180, null);
    }

    /** 验证可以按照席别和乘车区间筛选票价，并按照区间顺序返回。 */
    @Test
    void listTrainFares_shouldFilterAndOrderFares() {
        insertFare(1, 3, SeatType.SECOND_CLASS.getCode(), "300.00");
        insertFare(0, 2, SeatType.FIRST_CLASS.getCode(), "400.00");
        insertFare(0, 3, SeatType.SECOND_CLASS.getCode(), "553.00");

        List<TrainFare> result = trainFareService.listTrainFares(
                train.getId(),
                SeatType.SECOND_CLASS.getCode(),
                null,
                3
        );

        printFares("票价条件查询测试", result);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getFromStopIndex());
        assertEquals(1, result.get(1).getFromStopIndex());
        assertEquals(3, result.get(0).getToStopIndex());
        assertEquals(3, result.get(1).getToStopIndex());
        System.out.println("测试结果：通过");
    }

    /** 验证可以根据车次 ID 和票价 ID 查询指定票价。 */
    @Test
    void getTrainFare_shouldReturnSpecifiedFare() {
        TrainFare saved = insertFare(0, 3, SeatType.SECOND_CLASS.getCode(), "553.00");

        TrainFare result = trainFareService.getTrainFare(train.getId(), saved.getId());

        printFare("单条票价查询测试", result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(train.getId(), result.getTrainId());
        assertEquals(SeatType.SECOND_CLASS.getCode(), result.getSeatTypeCode());
        assertEquals(0, result.getPrice().compareTo(new BigDecimal("553.00")));
        System.out.println("测试结果：通过");
    }

    /** 验证新增票价后会生成雪花 ID，并正确保存席别、区间和金额。 */
    @Test
    void createTrainFare_shouldCreateFare() {
        TrainFare created = trainFareService.createTrainFare(
                train.getId(),
                new TrainFareCreateRequest(
                        SeatType.SECOND_CLASS.getCode(),
                        0,
                        3,
                        new BigDecimal("553.00")
                )
        );

        TrainFare saved = trainFareMapper.selectById(created.getId());
        printFare("新增票价测试", saved);
        assertNotNull(created.getId());
        assertNotNull(saved);
        assertEquals(train.getId(), saved.getTrainId());
        assertEquals(0, saved.getFromStopIndex());
        assertEquals(3, saved.getToStopIndex());
        assertEquals(0, saved.getPrice().compareTo(new BigDecimal("553.00")));
        System.out.println("测试结果：通过");
    }

    /** 验证同一车次、席别和乘车区间不能重复配置票价。 */
    @Test
    void createTrainFare_whenSameFareExists_shouldThrowConflict() {
        insertFare(0, 3, SeatType.SECOND_CLASS.getCode(), "553.00");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainFareService.createTrainFare(
                        train.getId(),
                        new TrainFareCreateRequest(
                                SeatType.SECOND_CLASS.getCode(),
                                0,
                                3,
                                new BigDecimal("560.00")
                        )
                )
        );

        System.out.println("========== 重复票价测试 ==========");
        System.out.println("异常状态码：" + exception.getCode());
        System.out.println("异常信息：" + exception.getMessage());
        assertEquals(409, exception.getCode());
        assertEquals("该车次、席别和乘车区间已配置票价", exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证修改票价时只更新金额，车次、席别和乘车区间保持不变。 */
    @Test
    void updateTrainFare_shouldOnlyUpdatePrice() {
        TrainFare saved = insertFare(0, 3, SeatType.SECOND_CLASS.getCode(), "553.00");

        TrainFare updated = trainFareService.updateTrainFare(
                train.getId(),
                saved.getId(),
                new TrainFareUpdateRequest(new BigDecimal("560.00"))
        );

        TrainFare result = trainFareMapper.selectById(saved.getId());
        printFare("修改票价测试", result);
        assertEquals(train.getId(), updated.getTrainId());
        assertEquals(SeatType.SECOND_CLASS.getCode(), updated.getSeatTypeCode());
        assertEquals(0, updated.getFromStopIndex());
        assertEquals(3, updated.getToStopIndex());
        assertEquals(0, result.getPrice().compareTo(new BigDecimal("560.00")));
        System.out.println("测试结果：通过");
    }

    /** 验证删除票价后，数据库中不再存在该票价记录。 */
    @Test
    void deleteTrainFare_shouldDeleteFare() {
        TrainFare saved = insertFare(0, 3, SeatType.SECOND_CLASS.getCode(), "553.00");

        trainFareService.deleteTrainFare(train.getId(), saved.getId());

        TrainFare result = trainFareMapper.selectById(saved.getId());
        System.out.println("========== 删除票价测试 ==========");
        System.out.println("已删除票价 ID：" + saved.getId());
        assertNull(result);
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

    private TrainFare insertFare(Integer fromStopIndex,
                                 Integer toStopIndex,
                                 String seatTypeCode,
                                 String price) {
        TrainFare trainFare = new TrainFare();
        trainFare.setTrainId(train.getId());
        trainFare.setSeatTypeCode(seatTypeCode);
        trainFare.setFromStopIndex(fromStopIndex);
        trainFare.setToStopIndex(toStopIndex);
        trainFare.setPrice(new BigDecimal(price));
        trainFareMapper.insert(trainFare);
        return trainFare;
    }

    private void printFares(String testName, List<TrainFare> fares) {
        System.out.println();
        System.out.println("========== " + testName + " ==========");
        System.out.println("测试车次 ID：" + train.getId());
        fares.forEach(fare -> System.out.printf(
                "fareId=%d, seatType=%s, route=%d->%d, price=%s%n",
                fare.getId(),
                fare.getSeatTypeCode(),
                fare.getFromStopIndex(),
                fare.getToStopIndex(),
                fare.getPrice()
        ));
    }

    private void printFare(String testName, TrainFare fare) {
        printFares(testName, List.of(fare));
    }
}
