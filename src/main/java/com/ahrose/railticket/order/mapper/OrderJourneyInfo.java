package com.ahrose.railticket.order.mapper;

import lombok.Data;

import java.time.LocalDate;

/** 订单关联的车次、车站及时刻信息，不对应独立数据表。 */
@Data
public class OrderJourneyInfo {
    private Long orderId;
    private Long trainId;
    private String trainNo;
    private String trainName;
    private LocalDate travelDate;
    private String fromStationCode;
    private String fromStationName;
    private Integer departureOffsetMinutes;
    private String toStationCode;
    private String toStationName;
    private Integer arrivalOffsetMinutes;
}
