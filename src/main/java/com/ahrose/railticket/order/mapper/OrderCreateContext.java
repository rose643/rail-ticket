package com.ahrose.railticket.order.mapper;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 创建订单联表查询结果，不对应单独的数据表。 */
@Data
public class OrderCreateContext {

    private Long scheduleId;
    private Long trainId;
    private LocalDate travelDate;
    private Integer scheduleStatus;
    private Integer trainStatus;
    private Long fromStopId;
    private Long toStopId;
    private Integer fromStationStatus;
    private Integer toStationStatus;
    private Integer departureOffsetMinutes;
    private Long fareId;
    private BigDecimal price;
}
