package com.ahrose.railticket.bean.response;

import com.ahrose.railticket.bean.TicketOrder;
import com.ahrose.railticket.common.util.SensitiveDataMasker;
import com.ahrose.railticket.order.mapper.OrderJourneyInfo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 订单和本人车票共用的响应，乘车人敏感信息已经脱敏。 */
public record TicketOrderResponse(
        Long id,
        String orderNo,
        Long passengerUserId,
        String passengerName,
        String passengerIdType,
        String passengerIdNo,
        String passengerPhone,
        Long scheduleId,
        String seatTypeCode,
        Integer fromStopIndex,
        Integer toStopIndex,
        Long trainId,
        String trainNo,
        String trainName,
        LocalDate travelDate,
        String fromStationCode,
        String fromStationName,
        String toStationCode,
        String toStationName,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Long durationMinutes,
        BigDecimal amount,
        String status,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 将订单和关联行程转换为包含完整乘车信息的响应。 */
    public static TicketOrderResponse from(TicketOrder order, OrderJourneyInfo journey) {
        LocalDateTime departureTime = journey.getTravelDate()
                .atStartOfDay()
                .plusMinutes(journey.getDepartureOffsetMinutes());
        LocalDateTime arrivalTime = journey.getTravelDate()
                .atStartOfDay()
                .plusMinutes(journey.getArrivalOffsetMinutes());
        return new TicketOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getPassengerUserId(),
                order.getPassengerName(),
                order.getPassengerIdType(),
                SensitiveDataMasker.maskIdNo(order.getPassengerIdNo()),
                SensitiveDataMasker.maskPhone(order.getPassengerPhone()),
                order.getScheduleId(),
                order.getSeatTypeCode(),
                order.getFromStopIndex(),
                order.getToStopIndex(),
                journey.getTrainId(),
                journey.getTrainNo(),
                journey.getTrainName(),
                journey.getTravelDate(),
                journey.getFromStationCode(),
                journey.getFromStationName(),
                journey.getToStationCode(),
                journey.getToStationName(),
                departureTime,
                arrivalTime,
                Duration.between(departureTime, arrivalTime).toMinutes(),
                order.getAmount(),
                order.getStatus(),
                order.getExpireAt(),
                order.getPaidAt(),
                order.getRefundedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
