package com.ahrose.railticket.bean.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 用户按出发站、到达站和始发日期查询到的一条车次结果。 */
public record TrainSearchResponse(
        Long scheduleId,
        Long trainId,
        String trainNo,
        String trainName,
        LocalDate travelDate,
        String fromStationCode,
        String fromStationName,
        Integer fromStopIndex,
        String toStationCode,
        String toStationName,
        Integer toStopIndex,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Long durationMinutes,
        List<SeatAvailabilityResponse> seats
) {
}
