package com.ahrose.railticket.train.stop.service;

import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.request.TrainStopCreateRequest;
import com.ahrose.railticket.bean.request.TrainStopUpdateRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TrainStopServiceTest {

    @Autowired
    private TrainStopService trainStopService;

    @Autowired
    private TrainStopMapper trainStopMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private TrainFareMapper trainFareMapper;

    private Train train;
    private Station firstStation;
    private Station secondStation;
    private Station thirdStation;

    /** 每个测试执行前创建一套独立测试数据，测试结束后由事务自动回滚。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        train = new Train();
        train.setTrainNo("TEST-" + suffix);
        train.setName("测试车次");
        train.setStatus(1);
        trainMapper.insert(train);

        firstStation = createStation("A" + suffix, "测试一站");
        secondStation = createStation("B" + suffix, "测试二站");
        thirdStation = createStation("C" + suffix, "测试三站");
    }

    /** 验证查询经停站时，会按照 stopIndex 从小到大返回结果。 */
    @Test
    void listTrainStops_shouldReturnStopsByStopIndex() {
        insertTrainStop(secondStation.getId(), 1, 60, 65);
        insertTrainStop(firstStation.getId(), 0, null, 0);

        List<TrainStop> result = trainStopService.listTrainStops(train.getId());

        printRoute("查询经停站测试", result);

        assertEquals(2, result.size());
        assertEquals(firstStation.getId(), result.get(0).getStationId());
        assertEquals(0, result.get(0).getStopIndex());
        assertEquals(secondStation.getId(), result.get(1).getStationId());
        assertEquals(1, result.get(1).getStopIndex());
        System.out.println("测试结果：通过");
    }

    /** 验证在路线中间新增经停站后，新站占据目标位置，原位置及后续站点自动后移。 */
    @Test
    void createTrainStop_shouldInsertAtSpecifiedIndexAndMoveLaterStops() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        insertTrainStop(thirdStation.getId(), 1, 120, null);

        TrainStop created = trainStopService.createTrainStop(
                train.getId(),
                new TrainStopCreateRequest(secondStation.getId(), 1, 60, 65)
        );

        List<TrainStop> result = trainStopService.listTrainStops(train.getId());

        printRoute("新增经停站测试", result);

        assertNotNull(created.getId());
        assertEquals(3, result.size());
        assertEquals(firstStation.getId(), result.get(0).getStationId());
        assertEquals(secondStation.getId(), result.get(1).getStationId());
        assertEquals(thirdStation.getId(), result.get(2).getStationId());
        assertEquals(List.of(0, 1, 2), result.stream().map(TrainStop::getStopIndex).toList());
        System.out.println("新增记录 ID：" + created.getId());
        System.out.println("测试结果：通过");
    }

    /** 验证修改经停站位置后，受影响的站点会重新排序，并保持 stopIndex 连续。 */
    @Test
    void updateTrainStop_shouldMoveStopAndKeepIndexesContinuous() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        insertTrainStop(secondStation.getId(), 1, 60, 65);
        TrainStop thirdStop = insertTrainStop(thirdStation.getId(), 2, 120, null);

        TrainStop updated = trainStopService.updateTrainStop(
                train.getId(),
                thirdStop.getId(),
                new TrainStopUpdateRequest(thirdStation.getId(), 0, 120, 125)
        );

        List<TrainStop> result = trainStopService.listTrainStops(train.getId());

        printRoute("修改经停站顺序测试", result);

        assertEquals(0, updated.getStopIndex());
        assertEquals(thirdStation.getId(), result.get(0).getStationId());
        assertEquals(firstStation.getId(), result.get(1).getStationId());
        assertEquals(secondStation.getId(), result.get(2).getStationId());
        assertEquals(List.of(0, 1, 2), result.stream().map(TrainStop::getStopIndex).toList());
        System.out.println("修改记录 ID：" + updated.getId());
        System.out.println("测试结果：通过");
    }

    /** 验证删除中间经停站后，后续站点自动前移，剩余 stopIndex 仍然连续。 */
    @Test
    void deleteTrainStop_shouldMoveLaterStopsForward() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        TrainStop secondStop = insertTrainStop(secondStation.getId(), 1, 60, 65);
        insertTrainStop(thirdStation.getId(), 2, 120, null);

        trainStopService.deleteTrainStop(train.getId(), secondStop.getId());

        List<TrainStop> result = trainStopService.listTrainStops(train.getId());

        printRoute("删除经停站测试", result);

        assertEquals(2, result.size());
        assertEquals(firstStation.getId(), result.get(0).getStationId());
        assertEquals(0, result.get(0).getStopIndex());
        assertEquals(thirdStation.getId(), result.get(1).getStationId());
        assertEquals(1, result.get(1).getStopIndex());
        System.out.println("删除记录 ID：" + secondStop.getId());
        System.out.println("测试结果：通过");
    }

    /** 验证车次已有票价时，不允许新增经停站。 */
    @Test
    void createTrainStop_whenFareExists_shouldThrowConflict() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        insertTrainStop(secondStation.getId(), 1, 60, null);
        insertFare(0, 1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainStopService.createTrainStop(
                        train.getId(),
                        new TrainStopCreateRequest(thirdStation.getId(), 1, 30, 35)
                )
        );

        assertRouteChangeBlocked(exception, "已有票价时新增经停站测试");
    }

    /** 验证车次已有票价时，不允许修改车站或经停顺序。 */
    @Test
    void updateTrainStopRoute_whenFareExists_shouldThrowConflict() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        insertTrainStop(secondStation.getId(), 1, 60, 65);
        TrainStop thirdStop = insertTrainStop(thirdStation.getId(), 2, 120, null);
        insertFare(0, 2);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainStopService.updateTrainStop(
                        train.getId(),
                        thirdStop.getId(),
                        new TrainStopUpdateRequest(thirdStation.getId(), 0, 120, 125)
                )
        );

        assertRouteChangeBlocked(exception, "已有票价时移动经停站测试");
    }

    /** 验证车次已有票价时，不允许删除经停站。 */
    @Test
    void deleteTrainStop_whenFareExists_shouldThrowConflict() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        TrainStop secondStop = insertTrainStop(secondStation.getId(), 1, 60, null);
        insertFare(0, 1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainStopService.deleteTrainStop(train.getId(), secondStop.getId())
        );

        assertRouteChangeBlocked(exception, "已有票价时删除经停站测试");
    }

    /** 验证已有票价时仍允许只调整到达、发车时间，不改变路线结构。 */
    @Test
    void updateTrainStopTime_whenFareExists_shouldSucceed() {
        insertTrainStop(firstStation.getId(), 0, null, 0);
        TrainStop secondStop = insertTrainStop(secondStation.getId(), 1, 60, null);
        insertFare(0, 1);

        TrainStop updated = trainStopService.updateTrainStop(
                train.getId(),
                secondStop.getId(),
                new TrainStopUpdateRequest(secondStation.getId(), 1, 70, null)
        );

        assertEquals(70, updated.getArrivalOffsetMinutes());
        assertEquals(1, updated.getStopIndex());
        System.out.println("========== 已有票价时修改时间测试 ==========");
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

    private TrainStop insertTrainStop(Long stationId,
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
        return trainStop;
    }

    private void insertFare(Integer fromStopIndex, Integer toStopIndex) {
        TrainFare trainFare = new TrainFare();
        trainFare.setTrainId(train.getId());
        trainFare.setSeatTypeCode(SeatType.SECOND_CLASS.getCode());
        trainFare.setFromStopIndex(fromStopIndex);
        trainFare.setToStopIndex(toStopIndex);
        trainFare.setPrice(new BigDecimal("100.00"));
        trainFareMapper.insert(trainFare);
    }

    private void assertRouteChangeBlocked(BusinessException exception, String testName) {
        System.out.println("========== " + testName + " ==========");
        System.out.println("异常状态码：" + exception.getCode());
        System.out.println("异常信息：" + exception.getMessage());
        assertEquals(409, exception.getCode());
        assertEquals("该车次已配置票价，请先删除票价后再修改路线", exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 将测试车次和经停站顺序输出到控制台，便于人工查看测试结果。 */
    private void printRoute(String testName, List<TrainStop> trainStops) {
        System.out.println();
        System.out.println("========== " + testName + " ==========");
        System.out.println("测试车次 ID：" + train.getId());
        for (TrainStop trainStop : trainStops) {
            System.out.printf(
                    "stopId=%d, stationId=%d, stopIndex=%d, arrival=%s, departure=%s%n",
                    trainStop.getId(),
                    trainStop.getStationId(),
                    trainStop.getStopIndex(),
                    trainStop.getArrivalOffsetMinutes(),
                    trainStop.getDepartureOffsetMinutes()
            );
        }
    }
}
