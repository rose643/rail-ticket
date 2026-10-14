package com.ahrose.railticket.train.search.service;

import com.ahrose.railticket.bean.SeatInventory;
import com.ahrose.railticket.bean.Station;
import com.ahrose.railticket.bean.Train;
import com.ahrose.railticket.bean.TrainFare;
import com.ahrose.railticket.bean.TrainSchedule;
import com.ahrose.railticket.bean.TrainStop;
import com.ahrose.railticket.bean.response.SeatAvailabilityResponse;
import com.ahrose.railticket.bean.response.TrainSearchResponse;
import com.ahrose.railticket.common.enums.SeatType;
import com.ahrose.railticket.common.enums.TrainSearchLocationType;
import com.ahrose.railticket.common.exception.BusinessException;
import com.ahrose.railticket.inventory.mapper.SeatInventoryMapper;
import com.ahrose.railticket.station.mapper.StationMapper;
import com.ahrose.railticket.train.base.mapper.TrainMapper;
import com.ahrose.railticket.train.fare.mapper.TrainFareMapper;
import com.ahrose.railticket.train.schedule.mapper.TrainScheduleMapper;
import com.ahrose.railticket.train.stop.mapper.TrainStopMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TrainSearchServiceTest {

    @Autowired
    private TrainSearchService trainSearchService;

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
    private StringRedisTemplate redisTemplate;

    private Train train;
    private TrainSchedule schedule;
    private String fromCode;
    private String toCode;
    private String fromCity;
    private String toCity;
    private LocalDate travelDate;

    /** 每个测试执行前准备一趟包含三个站、票价和区段库存的可售车次。 */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        fromCode = "F" + suffix;
        toCode = "T" + suffix;
        fromCity = "始发测试市" + suffix;
        toCity = "到达测试市" + suffix;
        travelDate = LocalDate.now().plusDays(2);

        train = new Train();
        train.setTrainNo("SEARCH-" + suffix);
        train.setName("查票测试车次");
        train.setStatus(1);
        trainMapper.insert(train);

        Station fromStation = createStation(fromCode, "查票测试始发站", fromCity);
        Station middleStation = createStation("M" + suffix, "查票测试中间站", "中间测试市" + suffix);
        Station toStation = createStation(toCode, "查票测试终点站", toCity);
        insertStop(fromStation.getId(), 0, null, 480);
        insertStop(middleStation.getId(), 1, 540, 545);
        insertStop(toStation.getId(), 2, 600, null);

        schedule = new TrainSchedule();
        schedule.setTrainId(train.getId());
        schedule.setTravelDate(travelDate);
        schedule.setStatus(1);
        scheduleMapper.insert(schedule);

        insertFare(SeatType.SECOND_CLASS.getCode(), new BigDecimal("120.00"));
        insertFare(SeatType.FIRST_CLASS.getCode(), new BigDecimal("180.00"));
        insertInventory(SeatType.SECOND_CLASS.getCode(), 0, 8);
        insertInventory(SeatType.SECOND_CLASS.getCode(), 1, 5);
        insertInventory(SeatType.FIRST_CLASS.getCode(), 0, 3);
        insertInventory(SeatType.FIRST_CLASS.getCode(), 1, 2);
    }

    /** 每个测试结束后清除本测试写入的查票缓存和索引，避免事务回滚后留下缓存数据。 */
    @AfterEach
    void clearSearchCache() {
        redisTemplate.delete(List.of(
                "train:search:data:" + travelDate + ":STATION:" + fromCode + ":STATION:" + toCode,
                "train:search:data:" + travelDate + ":CITY:" + fromCity + ":CITY:" + toCity,
                "train:search:index:date:" + travelDate,
                "train:search:index:schedule:" + schedule.getId(),
                "train:search:index:schedule:" + schedule.getId() + ":segment:0",
                "train:search:index:schedule:" + schedule.getId() + ":segment:1"
        ));
    }

    /** 验证查票结果包含车次时间、服务端票价以及乘车区间内的最小余票。 */
    @Test
    void searchTrains_shouldReturnFareAndMinimumSegmentAvailability() {
        List<TrainSearchResponse> result = trainSearchService.searchTrains(
                fromCode.toLowerCase(),
                toCode.toLowerCase(),
                travelDate
        );

        assertEquals(1, result.size());
        TrainSearchResponse response = result.getFirst();
        assertEquals(schedule.getId(), response.scheduleId());
        assertEquals(120L, response.durationMinutes());
        assertSeat(response, SeatType.SECOND_CLASS.getCode(), "120.00", 5);
        assertSeat(response, SeatType.FIRST_CLASS.getCode(), "180.00", 2);
        printSearchResult("查票票价与余票测试", response);
    }

    /** 验证相同查询条件第二次访问会读取 Redis 缓存，而不是立即使用数据库中的新库存值。 */
    @Test
    void searchTrains_whenCacheExists_shouldReturnCachedSnapshot() {
        TrainSearchResponse first = trainSearchService.searchTrains(
                fromCode,
                toCode,
                travelDate
        ).getFirst();

        List<SeatInventory> inventories = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeatInventory>()
                        .eq(SeatInventory::getScheduleId, schedule.getId())
                        .eq(SeatInventory::getSeatTypeCode, SeatType.SECOND_CLASS.getCode())
        );
        inventories.forEach(inventory -> {
            inventory.setAvailable(1);
            inventoryMapper.updateById(inventory);
        });

        TrainSearchResponse second = trainSearchService.searchTrains(
                fromCode,
                toCode,
                travelDate
        ).getFirst();

        assertSeat(first, SeatType.SECOND_CLASS.getCode(), "120.00", 5);
        assertSeat(second, SeatType.SECOND_CLASS.getCode(), "120.00", 5);
        printSearchResult("查票 Redis 缓存命中测试", second);
    }

    /** 验证选择出发城市和到达城市时，会查询城市内的具体上下车站。 */
    @Test
    void searchTrains_byCities_shouldReturnActualStations() {
        TrainSearchResponse response = trainSearchService.searchTrains(
                TrainSearchLocationType.CITY,
                fromCity,
                TrainSearchLocationType.CITY,
                toCity,
                travelDate
        ).getFirst();

        assertEquals(fromCode, response.fromStationCode());
        assertEquals(toCode, response.toStationCode());
        assertEquals("查票测试始发站", response.fromStationName());
        assertEquals("查票测试终点站", response.toStationName());
        printSearchResult("按城市查票测试", response);
    }

    /** 验证城市与具体车站可以组合为一次查票条件。 */
    @Test
    void searchTrains_byCityAndStation_shouldReturnMatchedTrain() {
        List<TrainSearchResponse> result = trainSearchService.searchTrains(
                TrainSearchLocationType.CITY,
                fromCity,
                TrainSearchLocationType.STATION,
                toCode.toLowerCase(),
                travelDate
        );

        assertEquals(1, result.size());
        assertEquals(fromCode, result.getFirst().fromStationCode());
        assertEquals(toCode, result.getFirst().toStationCode());
        printSearchResult("城市到具体车站查票测试", result.getFirst());
    }

    /** 验证出发站和到达站相同时，查票服务会拒绝请求。 */
    @Test
    void searchTrains_whenStationsSame_shouldThrowBadRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainSearchService.searchTrains(fromCode, fromCode, travelDate)
        );

        assertEquals(400, exception.getCode());
        assertEquals("出发地点和到达地点不能相同", exception.getMessage());
        System.out.println("========== 相同车站查票测试 ==========");
        System.out.println("异常信息：" + exception.getMessage());
        System.out.println("测试结果：通过");
    }

    /** 验证早于当前日期的乘车日期不能用于查票。 */
    @Test
    void searchTrains_whenTravelDateExpired_shouldThrowBadRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> trainSearchService.searchTrains(
                        fromCode,
                        toCode,
                        LocalDate.now().minusDays(1)
                )
        );

        assertEquals(400, exception.getCode());
        assertEquals("始发日期不能早于当前日期", exception.getMessage());
        System.out.println("========== 过期日期查票测试 ==========");
        System.out.println("异常信息：" + exception.getMessage());
        System.out.println("测试结果：通过");
    }

    private Station createStation(String code, String name, String city) {
        Station station = new Station();
        station.setCode(code);
        station.setName(name);
        station.setCity(city);
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

    private void insertFare(String seatTypeCode, BigDecimal price) {
        TrainFare fare = new TrainFare();
        fare.setTrainId(train.getId());
        fare.setSeatTypeCode(seatTypeCode);
        fare.setFromStopIndex(0);
        fare.setToStopIndex(2);
        fare.setPrice(price);
        fareMapper.insert(fare);
    }

    private void insertInventory(String seatTypeCode, Integer segmentIndex, Integer available) {
        SeatInventory inventory = new SeatInventory();
        inventory.setScheduleId(schedule.getId());
        inventory.setSeatTypeCode(seatTypeCode);
        inventory.setSegmentIndex(segmentIndex);
        inventory.setTotal(10);
        inventory.setAvailable(available);
        inventoryMapper.insert(inventory);
    }

    private void assertSeat(
            TrainSearchResponse response,
            String seatTypeCode,
            String price,
            int available
    ) {
        SeatAvailabilityResponse seat = response.seats().stream()
                .filter(item -> seatTypeCode.equals(item.seatTypeCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal(price), seat.price());
        assertEquals(available, seat.available());
    }

    private void printSearchResult(String testName, TrainSearchResponse response) {
        System.out.println("========== " + testName + " ==========");
        System.out.printf(
                "scheduleId=%d, trainNo=%s, from=%s, to=%s, duration=%d%n",
                response.scheduleId(),
                response.trainNo(),
                response.fromStationCode(),
                response.toStationCode(),
                response.durationMinutes()
        );
        response.seats().forEach(seat -> System.out.printf(
                "seatType=%s, price=%s, available=%d%n",
                seat.seatTypeCode(), seat.price(), seat.available()
        ));
        System.out.println("测试结果：通过");
    }
}
